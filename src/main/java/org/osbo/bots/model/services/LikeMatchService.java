package org.osbo.bots.model.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.osbo.bots.util.LookingForOption;
import org.osbo.bots.util.MarkdownEscaper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Processes likes, detects mutual matches, sends notifications and lists matches.
 */
@Slf4j
@Component
public class LikeMatchService {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String USER_STATUS_BLOCKED = "bloqueado";

    public static final String MESSAGE_ANONYMOUS_LIKE = "A alguien le gustó tu perfil. Entrá al club con /ver_personas para seguir descubriendo.";
    public static final String MESSAGE_UNAVAILABLE_PROFILE = "El perfil que te gustó no está disponible ahora. Continuá con /ver_personas.";

    public static final String MESSAGE_NO_MATCHES = "Todavía no tenés matches. Seguí descubriendo personas con /ver_personas.";
    public static final String MESSAGE_MATCHES_REQUIRES_APPROVAL = "Necesitás tener un perfil aprobado para ver tus matches. Registrate con /club y esperá la aprobación.";
    public static final String MESSAGE_REPORT_CONFIRMATION = "Perfil reportado. Los administradores lo revisarán.";

    public static final String CALLBACK_REPORT_PREFIX = "club_match_report_";
    public static final String MODERATION_TYPE_REPORT = "REPORT";
    public static final String REASON_REPORT_FROM_MATCH = "Perfil reportado desde match";

    public static final String SEND_TYPE_MATCH_NOTIFICATION = "match_notification";

    public static final int MATCHES_PER_PAGE = 3;
    public static final String CALLBACK_MATCHES_PAGE_PREFIX = "matches_page_";
    public static final String CALLBACK_MATCHES_CLEAR_PREFIX = "matches_clear";
    public static final String CALLBACK_MATCHES_BACK_PREFIX = "matches_back";

    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";
    public static final String ORIENTATION_HETERO = "HETERO";
    public static final String ORIENTATION_BI = "BI";

    private final NqueueForSend sender;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final JmsTemplate jmsTemplate;

    public LikeMatchService(NqueueForSend sender, ProfileRepository profileRepository,
            UserRepository userRepository, LikeRepository likeRepository, JmsTemplate jmsTemplate) {
        this.sender = sender;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Processes a like from {@code queue.like}: validates the target, persists the
     * like, detects mutual matches and sends the appropriate notification.
     *
     * @param message the incoming like message
     */
    @Transactional
    public void processLike(LikeMessage message) {
        if (message == null || message.getFromChatid() == null || message.getToChatid() == null) {
            return;
        }

        String fromChatid = message.getFromChatid();
        String toChatid = message.getToChatid();
        if (fromChatid.equals(toChatid)) {
            return;
        }

        Profile targetProfile = profileRepository.findByChatid(toChatid);
        User targetUser = userRepository.findById(toChatid).orElse(null);
        if (targetProfile == null || !STATUS_APPROVED.equals(targetProfile.getStatus())
                || (targetUser != null && USER_STATUS_BLOCKED.equals(targetUser.getEstado()))) {
            sender.send(fromChatid, MESSAGE_UNAVAILABLE_PROFILE);
            return;
        }

        if (likeRepository.findByFromChatidAndToChatid(fromChatid, toChatid) != null) {
            return;
        }

        Like reverse = likeRepository.findByFromChatidAndToChatid(toChatid, fromChatid);

        Like like = new Like();
        like.setFromChatid(fromChatid);
        like.setToChatid(toChatid);
        like.setMatched(reverse != null);
        like.setCreatedAt(isoTimestamp());

        try {
            likeRepository.save(like);
            if (reverse != null) {
                reverse.setMatched(true);
                likeRepository.save(reverse);
            }
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate like ignored: from {} to {}", fromChatid, toChatid);
            return;
        }

        if (reverse != null) {
            jmsTemplate.convertAndSend("queue.match", new MatchMessage(fromChatid, toChatid));
        } else {
            sender.send(toChatid, MESSAGE_ANONYMOUS_LIKE);
        }
    }

    /**
     * Sends a rich match notification to both users.
     *
     * @param message the match message
     */
    public void notifyMatch(MatchMessage message) {
        if (message == null || message.getChatidA() == null || message.getChatidB() == null) {
            return;
        }

        Profile profileA = profileRepository.findByChatid(message.getChatidA());
        Profile profileB = profileRepository.findByChatid(message.getChatidB());
        if (profileA == null || profileB == null) {
            return;
        }

        sendMatchNotification(profileA, profileB);
        sendMatchNotification(profileB, profileA);
    }

    private void sendMatchNotification(Profile viewer, Profile matched) {
        String caption = buildMatchCaption(matched);
        List<List<Button>> buttons = buildMatchButtons(matched);
        if (matched.getPhotoFileId() == null || matched.getPhotoFileId().isBlank()) {
            sender.send(viewer.getChatid(), caption, "text", null, null, false, buttons, null, "Markdown");
        } else {
            sender.send(viewer.getChatid(), caption, SEND_TYPE_MATCH_NOTIFICATION, matched.getPhotoFileId(), null, false,
                    buttons, null, "Markdown");
        }
    }

    private String buildMatchCaption(Profile profile) {
        StringBuilder caption = new StringBuilder();
        caption.append("🎉 *¡Es un match!* ❤️\n\n");
        caption.append("*").append(MarkdownEscaper.escape(profile.getName())).append("*, ")
                .append(profile.getAge()).append(" años\n");
        caption.append(translateGender(profile.getGender())).append(" · ")
                .append(translateOrientation(profile.getOrientation())).append("\n");
        caption.append("📍 ").append(MarkdownEscaper.escape(profile.getCity())).append("\n\n");
        caption.append("*📝 Sobre:* ").append(MarkdownEscaper.escape(profile.getDescription())).append("\n");
        caption.append("*🎸 Gustos:* ").append(MarkdownEscaper.escape(profile.getTastes())).append("\n");
        caption.append("*🧠 Personalidad:* ").append(MarkdownEscaper.escape(profile.getTraits())).append("\n");
        caption.append("*💘 Buscando:* ").append(LookingForOption.translate(profile.getLookingFor()));
        return caption.toString();
    }

    private List<List<Button>> buildMatchButtons(Profile matched) {
        List<Button> contactButtons = new ArrayList<>();
        if (matched.getContactUsername() != null && !matched.getContactUsername().isBlank()) {
            contactButtons.add(new Button("✉️ Telegram @" + matched.getContactUsername(), null,
                    "https://t.me/" + matched.getContactUsername()));
        }
        if (matched.getWhatsapp() != null && !matched.getWhatsapp().isBlank()) {
            String normalized = matched.getWhatsapp().replaceAll("[^0-9]", "");
            contactButtons.add(new Button("📱 WhatsApp " + matched.getWhatsapp(), null,
                    "https://wa.me/" + normalized));
        }
        List<List<Button>> rows = new ArrayList<>();
        if (!contactButtons.isEmpty()) {
            rows.add(contactButtons);
        }
        rows.add(Arrays.asList(new Button("🚫 Reportar", CALLBACK_REPORT_PREFIX + matched.getChatid(), null)));
        return rows;
    }

    /**
     * Lists the current matches for the given chat ID starting at page 0.
     *
     * @param chatid the chat ID of the user
     */
    public void listMatches(String chatid) {
        showMatchListPage(chatid, 0, null);
    }

    /**
     * Handles pagination and clear callbacks for the match list.
     *
     * @param user   the current user
     * @param update the Telegram update
     * @return true if the callback was handled
     */
    public boolean handleMatchListCallback(User user, MessageUpdate update) {
        String text = update.getText();
        if (text == null || update.getMessageId() == null) {
            return false;
        }
        if (text.startsWith(CALLBACK_MATCHES_PAGE_PREFIX)) {
            String pageStr = text.substring(CALLBACK_MATCHES_PAGE_PREFIX.length());
            int page = Integer.parseInt(pageStr);
            showMatchListPage(user.getChatid(), page, update.getMessageId());
            return true;
        }
        if (CALLBACK_MATCHES_CLEAR_PREFIX.equals(text)) {
            clearMatches(user.getChatid(), update.getMessageId());
            return true;
        }
        return false;
    }

    private void showMatchListPage(String chatid, int page, Integer messageIdToDelete) {
        if (chatid == null || chatid.isBlank()) {
            return;
        }

        Profile profile = profileRepository.findByChatid(chatid);
        if (profile == null || !STATUS_APPROVED.equals(profile.getStatus())) {
            sender.send(chatid, MESSAGE_MATCHES_REQUIRES_APPROVAL);
            return;
        }

        List<Profile> uniqueMatches = findUniqueMatches(chatid);
        if (uniqueMatches.isEmpty()) {
            sender.send(chatid, MESSAGE_NO_MATCHES);
            return;
        }

        int totalPages = (int) Math.ceil((double) uniqueMatches.size() / MATCHES_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int start = safePage * MATCHES_PER_PAGE;
        int end = Math.min(start + MATCHES_PER_PAGE, uniqueMatches.size());
        List<Profile> pageMatches = uniqueMatches.subList(start, end);

        String text = buildMatchListText(pageMatches, safePage, totalPages);
        List<List<Button>> buttons = buildMatchListNavigationButtons(safePage, totalPages);

        if (messageIdToDelete != null) {
            sender.deleteMessage(chatid, messageIdToDelete);
        }
        sender.send(chatid, text, "text", null, null, false, buttons, null);
    }

    private List<Profile> findUniqueMatches(String chatid) {
        List<Like> matches = likeRepository.findByFromChatidOrToChatidAndMatchedTrue(chatid);
        Set<String> seen = new HashSet<>();
        List<Profile> unique = new ArrayList<>();
        for (Like like : matches) {
            String otherChatid = chatid.equals(like.getFromChatid()) ? like.getToChatid() : like.getFromChatid();
            if (seen.add(otherChatid)) {
                Profile other = profileRepository.findByChatid(otherChatid);
                if (other != null) {
                    unique.add(other);
                }
            }
        }
        return unique;
    }

    private String buildMatchListText(List<Profile> matches, int page, int totalPages) {
        StringBuilder text = new StringBuilder();
        text.append("*Tus matches* (página ").append(page + 1).append(" de ").append(totalPages).append(")\n\n");
        if (matches.isEmpty()) {
            text.append("No hay matches en esta página.");
            return text.toString();
        }
        for (Profile other : matches) {
            appendMatchEntry(text, other);
        }
        return text.toString();
    }

    private void appendMatchEntry(StringBuilder text, Profile other) {
        text.append("• *").append(MarkdownEscaper.escape(other.getName())).append("*, ")
                .append(other.getAge()).append(" años · ")
                .append(MarkdownEscaper.escape(other.getCity())).append("\n");
        if (other.getContactUsername() != null && !other.getContactUsername().isBlank()) {
            text.append("  ✉️ Telegram: @").append(MarkdownEscaper.escape(other.getContactUsername())).append("\n");
        }
        if (other.getWhatsapp() != null && !other.getWhatsapp().isBlank()) {
            text.append("  📱 WhatsApp: ").append(MarkdownEscaper.escape(other.getWhatsapp())).append("\n");
        }
        text.append("\n");
    }

    private List<List<Button>> buildMatchListNavigationButtons(int page, int totalPages) {
        List<Button> navigation = new ArrayList<>();
        if (page > 0) {
            navigation.add(new Button("⬅️ Anterior", CALLBACK_MATCHES_PAGE_PREFIX + (page - 1)));
        }
        if (page < totalPages - 1) {
            navigation.add(new Button("➡️ Siguiente", CALLBACK_MATCHES_PAGE_PREFIX + (page + 1)));
        }
        List<Button> actions = new ArrayList<>();
        actions.add(new Button("🗑️ Limpiar matches", CALLBACK_MATCHES_CLEAR_PREFIX));
        actions.add(new Button("🏠 Volver al inicio", CALLBACK_MATCHES_BACK_PREFIX));

        List<List<Button>> rows = new ArrayList<>();
        if (!navigation.isEmpty()) {
            rows.add(navigation);
        }
        rows.add(actions);
        return rows;
    }

    private void clearMatches(String chatid, int messageId) {
        List<Like> matches = likeRepository.findByFromChatidOrToChatidAndMatchedTrue(chatid);
        likeRepository.deleteAll(matches);
        sender.deleteMessage(chatid, messageId);
        sender.send(chatid, "🗑️ *Tus matches han sido limpiados.*\n\nPodés seguir descubriendo personas con /ver_personas.");
    }

    /**
     * Reports a matched profile to the moderation queue.
     *
     * @param reporterChatid the chat ID of the user reporting
     * @param reportedChatid the chat ID of the reported profile
     */
    public void reportMatch(String reporterChatid, String reportedChatid) {
        Profile reported = profileRepository.findByChatid(reportedChatid);
        ModerationMessage message = new ModerationMessage();
        message.setType(MODERATION_TYPE_REPORT);
        message.setChatid(reportedChatid);
        message.setReason(REASON_REPORT_FROM_MATCH);
        if (reported != null) {
            message.setName(reported.getName());
            message.setBirthDate(reported.getBirthDate() != null ? reported.getBirthDate().toString() : null);
            message.setGender(reported.getGender());
            message.setOrientation(reported.getOrientation());
            message.setCountry(reported.getCountry());
            message.setCity(reported.getCity());
            message.setDescription(reported.getDescription());
            message.setTastes(reported.getTastes());
            message.setTraits(reported.getTraits());
            message.setLookingFor(reported.getLookingFor());
            message.setPhotoFileId(reported.getPhotoFileId());
            message.setContactUsername(reported.getContactUsername());
            message.setWhatsapp(reported.getWhatsapp());
        }
        jmsTemplate.convertAndSend("queue.moderation", message);
        sender.send(reporterChatid, MESSAGE_REPORT_CONFIRMATION);
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case GENDER_MALE -> "Hombre";
            case GENDER_FEMALE -> "Mujer";
            default -> gender;
        };
    }

    private String translateOrientation(String orientation) {
        return switch (orientation) {
            case ORIENTATION_HETERO -> "Hetero";
            case ORIENTATION_BI -> "Bi";
            default -> orientation;
        };
    }

    private String isoTimestamp() {
        return OffsetDateTime.now().toString();
    }

}
