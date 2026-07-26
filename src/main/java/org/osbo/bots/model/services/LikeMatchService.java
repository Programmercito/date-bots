package org.osbo.bots.model.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
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
            sender.send(viewer.getChatid(), caption, "text", null, null, false, buttons, null);
        } else {
            sender.send(viewer.getChatid(), caption, SEND_TYPE_MATCH_NOTIFICATION, matched.getPhotoFileId(), null, false,
                    buttons, null);
        }
    }

    private String buildMatchCaption(Profile profile) {
        StringBuilder caption = new StringBuilder();
        caption.append("¡Es un match! ❤️\n\n");
        caption.append(profile.getName()).append(", ").append(profile.getAge()).append(" años\n");
        caption.append(translateGender(profile.getGender())).append(" · ")
                .append(translateOrientation(profile.getOrientation())).append("\n");
        caption.append("📍 ").append(profile.getCity()).append("\n\n");
        caption.append("Sobre: ").append(profile.getDescription()).append("\n");
        caption.append("Gustos: ").append(profile.getTastes()).append("\n");
        caption.append("Personalidad: ").append(profile.getTraits()).append("\n");
        caption.append("Buscando: ").append(profile.getLookingFor());
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
     * Lists the current matches for the given chat ID.
     *
     * @param chatid the chat ID of the user
     */
    public void listMatches(String chatid) {
        if (chatid == null || chatid.isBlank()) {
            return;
        }

        Profile profile = profileRepository.findByChatid(chatid);
        if (profile == null || !STATUS_APPROVED.equals(profile.getStatus())) {
            sender.send(chatid, MESSAGE_MATCHES_REQUIRES_APPROVAL);
            return;
        }

        List<Like> matches = likeRepository.findByFromChatidOrToChatidAndMatchedTrue(chatid);
        if (matches.isEmpty()) {
            sender.send(chatid, MESSAGE_NO_MATCHES);
            return;
        }

        StringBuilder text = new StringBuilder("Tus matches:\n\n");
        List<List<Button>> buttons = new ArrayList<>();
        for (Like like : matches) {
            String otherChatid = chatid.equals(like.getFromChatid()) ? like.getToChatid() : like.getFromChatid();
            Profile other = profileRepository.findByChatid(otherChatid);
            if (other == null) {
                continue;
            }
            appendMatchEntry(text, other);
            buttons.addAll(buildMatchButtons(other));
        }
        sender.send(chatid, text.toString(), "text", null, null, false, buttons, null);
    }

    private void appendMatchEntry(StringBuilder text, Profile other) {
        text.append("• ").append(other.getName()).append(", ").append(other.getAge()).append(" años · ")
                .append(other.getCity()).append("\n");
        if (other.getContactUsername() != null && !other.getContactUsername().isBlank()) {
            text.append("  Telegram: @").append(other.getContactUsername()).append("\n");
        }
        if (other.getWhatsapp() != null && !other.getWhatsapp().isBlank()) {
            text.append("  WhatsApp: ").append(other.getWhatsapp()).append("\n");
        }
        text.append("\n");
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
