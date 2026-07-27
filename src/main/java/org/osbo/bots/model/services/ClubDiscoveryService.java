package org.osbo.bots.model.services;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.AnalyticsMessage;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.Report;
import org.osbo.bots.model.entity.SkippedProfile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.ReportRepository;
import org.osbo.bots.model.repositories.SkippedProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.osbo.bots.util.FechaActual;
import org.osbo.bots.util.LookingForOption;
import org.osbo.bots.util.MarkdownEscaper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

/**
 * Handles the friendship club discovery flow: showing approved profiles one at a
 * time and processing like/skip/report actions.
 */
@Component
public class ClubDiscoveryService {

    public static final String COMMAND_VIEW_PEOPLE = "/ver_personas";
    public static final String CALLBACK_NEXT = "club_next";
    public static final String CALLBACK_LIKE_PREFIX = "club_like_";
    public static final String CALLBACK_SKIP_PREFIX = "club_skip_";
    public static final String CALLBACK_REPORT_PREFIX = "club_report_";

    public static final String STATE_BROWSING = "club_browsing";
    public static final String STATE_NO_PROFILES = "club_no_profiles";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_BLOCKED = "bloqueado";
    public static final String COUNTRY_BOLIVIA = "BO";

    public static final String EVENT_LIKE = "LIKE";
    public static final String EVENT_VIEW = "VIEW";
    public static final int SKIP_COOLDOWN_DAYS = 10;

    public static final String REPORT_STATUS_OPEN = "OPEN";
    public static final String REPORT_REASON_PROFILE = "Perfil reportado desde descubrimiento";
    public static final String MODERATION_TYPE_REPORT = "REPORT";

    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";
    public static final String ORIENTATION_HETERO = "HETERO";
    public static final String ORIENTATION_BI = "BI";

    private static final String SEND_TYPE_DISCOVERY_PROFILE = "discovery_profile";
    private static final String SEND_TYPE_DISCOVERY_EMPTY = "discovery_empty";
    private static final DateTimeFormatter NOTICE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NqueueForSend sender;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ReportRepository reportRepository;
    private final SkippedProfileRepository skippedProfileRepository;
    private final JmsTemplate jmsTemplate;

    public ClubDiscoveryService(NqueueForSend sender, ProfileRepository profileRepository,
            UserRepository userRepository, LikeRepository likeRepository,
            ReportRepository reportRepository, SkippedProfileRepository skippedProfileRepository, JmsTemplate jmsTemplate) {
        this.sender = sender;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.reportRepository = reportRepository;
        this.skippedProfileRepository = skippedProfileRepository;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Dispatches discovery commands and callbacks.
     *
     * @param user   the current user
     * @param update the Telegram update
     * @return true if the message was handled by this service
     */
    public boolean handle(@NonNull User user, @NonNull MessageUpdate update) {
        String text = update.getText();
        if (text == null) {
            return false;
        }

        if (COMMAND_VIEW_PEOPLE.equals(text) || CALLBACK_NEXT.equals(text)) {
            return handleViewPeople(user, update);
        }

        if (STATE_BROWSING.equals(user.getComando())) {
            if (text.startsWith(CALLBACK_LIKE_PREFIX)) {
                handleLike(user, update);
                return true;
            }
            if (text.startsWith(CALLBACK_SKIP_PREFIX)) {
                handleSkip(user, update);
                return true;
            }
            if (text.startsWith(CALLBACK_REPORT_PREFIX)) {
                handleReport(user, update);
                return true;
            }
        }

        return false;
    }

    private boolean handleViewPeople(User user, MessageUpdate update) {
        Profile profile = profileRepository.findByChatid(user.getChatid());
        if (profile == null || !STATUS_APPROVED.equals(profile.getStatus())) {
            sender.send(update.getChatid(),
                    "Necesitás tener un perfil aprobado para ver personas. Registrate con /club y esperá la aprobación.");
            return true;
        }
        showNextProfile(user, update);
        return true;
    }

    /**
     * Shows the next approved profile matching the user's filters.
     *
     * @param user   the current user
     * @param update the Telegram update
     */
    public void showNextProfile(User user, MessageUpdate update) {
        Profile next = findNextProfile(user.getChatid());
        if (next == null) {
            String notice = noProfilesNotice();
            if (STATE_NO_PROFILES.equals(user.getComando())) {
                if (user.getCurrentProfileMessageId() != null) {
                    deleteCurrentProfileMessage(user);
                    user.setCurrentProfileMessageId(null);
                    sender.send(update.getChatid(), notice, SEND_TYPE_DISCOVERY_EMPTY, null, null, false);
                }
                userRepository.save(user);
                return;
            }
            deletePreviousProfileMessage(user);
            deleteCurrentProfileMessage(user);
            user.setPreviousProfileMessageId(null);
            user.setCurrentProfileMessageId(null);
            sender.send(update.getChatid(), notice, SEND_TYPE_DISCOVERY_EMPTY, null, null, false);
            user.setComando(STATE_NO_PROFILES);
            userRepository.save(user);
            return;
        }

        user.setComando(STATE_BROWSING);
        String caption = buildProfileCaption(next);
        List<List<Button>> buttons = buildProfileButtons(next);
        deletePreviousProfileMessage(user);
        deleteCurrentProfileMessage(user);
        user.setPreviousProfileMessageId(null);
        user.setCurrentProfileMessageId(null);
        sender.send(update.getChatid(), caption, SEND_TYPE_DISCOVERY_PROFILE, next.getPhotoFileId(), null, false,
                buttons, next.getChatid(), "Markdown");
        sendAnalytics(update.getChatid(), EVENT_VIEW, 1);
        userRepository.save(user);
    }

    /**
     * Processes a like callback: edits the current message, sends the like to
     * queue.like and tracks the analytics event.
     *
     * @param user   the current user
     * @param update the Telegram update
     */
    public void handleLike(User user, MessageUpdate update) {
        String targetChatid = extractTargetChatid(update.getText(), CALLBACK_LIKE_PREFIX);
        Profile profile = profileRepository.findByChatid(targetChatid);
        String name = profile == null ? "alguien" : profile.getName();

        trackCurrentMessageId(user, update);
        String confirmation = "Le diste like a " + name + " ❤️";
        List<List<Button>> buttons = List.of(List.of(new Button("Siguiente ➡️", CALLBACK_NEXT)));
        sender.editCaption(update.getChatid(), user.getCurrentProfileMessageId(), confirmation, buttons);

        sendLikeMessage(update.getChatid(), targetChatid);
        sendAnalytics(update.getChatid(), EVENT_LIKE, 1);
        userRepository.save(user);
    }

    /**
     * Processes a skip callback: edits the current message and tracks the view.
     *
     * @param user   the current user
     * @param update the Telegram update
     */
    public void handleSkip(User user, MessageUpdate update) {
        String targetChatid = extractTargetChatid(update.getText(), CALLBACK_SKIP_PREFIX);
        Profile profile = profileRepository.findByChatid(targetChatid);
        String name = profile == null ? "alguien" : profile.getName();

        trackCurrentMessageId(user, update);
        String confirmation = "Skippeaste a " + name + " 👋";
        List<List<Button>> buttons = List.of(List.of(new Button("Siguiente ➡️", CALLBACK_NEXT)));
        sender.editCaption(update.getChatid(), user.getCurrentProfileMessageId(), confirmation, buttons);

        saveSkip(update.getChatid(), targetChatid);
        sendAnalytics(update.getChatid(), EVENT_VIEW, 1);
        userRepository.save(user);
    }

    /**
     * Processes a report callback: saves the report, notifies the admin and edits
     * the current message.
     *
     * @param user   the current user
     * @param update the Telegram update
     */
    public void handleReport(User user, MessageUpdate update) {
        String targetChatid = extractTargetChatid(update.getText(), CALLBACK_REPORT_PREFIX);
        Profile profile = profileRepository.findByChatid(targetChatid);

        trackCurrentMessageId(user, update);
        saveReport(update.getChatid(), targetChatid);
        notifyAdminAboutReport(profile);

        String confirmation = "Perfil reportado. Los administradores lo revisarán.";
        List<List<Button>> buttons = List.of(List.of(new Button("Siguiente ➡️", CALLBACK_NEXT)));
        sender.editCaption(update.getChatid(), user.getCurrentProfileMessageId(), confirmation, buttons);

        userRepository.save(user);
    }

    private Profile findNextProfile(String currentChatid) {
        Profile currentProfile = profileRepository.findByChatid(currentChatid);
        if (currentProfile == null) {
            return null;
        }

        List<Profile> approved = profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(STATUS_APPROVED,
                COUNTRY_BOLIVIA);
        List<String> alreadyLiked = likeRepository.findByFromChatid(currentChatid).stream()
                .map(Like::getToChatid)
                .toList();
        List<String> activeSkips = skippedProfileRepository
                .findByFromChatidAndExpiresAtAfter(currentChatid, Instant.now().toString())
                .stream()
                .map(SkippedProfile::getToChatid)
                .toList();

        return approved.stream()
                .filter(p -> !p.getChatid().equals(currentChatid))
                .filter(p -> matchesFilters(currentProfile, p))
                .filter(p -> matchesCity(currentProfile, p))
                .filter(p -> !alreadyLiked.contains(p.getChatid()))
                .filter(p -> !activeSkips.contains(p.getChatid()))
                .filter(p -> isActiveUser(p.getChatid()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesCity(Profile viewer, Profile target) {
        String viewerCity = viewer.getCity();
        String targetCity = target.getCity();
        if (viewerCity == null || viewerCity.isBlank() || targetCity == null || targetCity.isBlank()) {
            return true;
        }
        return viewerCity.trim().equalsIgnoreCase(targetCity.trim());
    }

    private boolean matchesFilters(Profile viewer, Profile target) {
        String viewerGender = viewer.getGender();
        String viewerOrientation = viewer.getOrientation();
        String targetGender = target.getGender();
        String targetOrientation = target.getOrientation();

        if (GENDER_MALE.equals(viewerGender) && ORIENTATION_HETERO.equals(viewerOrientation)) {
            return GENDER_FEMALE.equals(targetGender) && ORIENTATION_HETERO.equals(targetOrientation);
        }
        if (GENDER_FEMALE.equals(viewerGender) && ORIENTATION_HETERO.equals(viewerOrientation)) {
            return GENDER_MALE.equals(targetGender) && ORIENTATION_HETERO.equals(targetOrientation);
        }
        if (GENDER_FEMALE.equals(viewerGender) && ORIENTATION_BI.equals(viewerOrientation)) {
            return (GENDER_FEMALE.equals(targetGender)
                    && (ORIENTATION_HETERO.equals(targetOrientation) || ORIENTATION_BI.equals(targetOrientation)))
                    || (GENDER_MALE.equals(targetGender) && ORIENTATION_HETERO.equals(targetOrientation));
        }
        return false;
    }

    private boolean isActiveUser(String chatid) {
        User user = userRepository.findById(chatid).orElse(null);
        return user != null && !STATUS_BLOCKED.equals(user.getEstado());
    }

    private String buildProfileCaption(Profile profile) {
        int photoCount = profile.photoCount();
        String photoLine = photoCount > 1 ? "📸 " + photoCount + " fotos\n" : "";
        return "*" + MarkdownEscaper.escape(profile.getName()) + "*, " + profile.getAge() + " años\n"
                + translateGender(profile.getGender()) + " · " + translateOrientation(profile.getOrientation()) + "\n"
                + "📍 " + MarkdownEscaper.escape(profile.getCity()) + "\n"
                + photoLine
                + "\n*📝 Sobre:* " + MarkdownEscaper.escape(profile.getDescription()) + "\n"
                + "*🎸 Gustos:* " + MarkdownEscaper.escape(profile.getTastes()) + "\n"
                + "*🧠 Personalidad:* " + MarkdownEscaper.escape(profile.getTraits()) + "\n"
                + "*💘 Buscando:* " + LookingForOption.translate(profile.getLookingFor());
    }

    private List<List<Button>> buildProfileButtons(Profile profile) {
        return Arrays.asList(
                Arrays.asList(
                        new Button("❤️ Like", CALLBACK_LIKE_PREFIX + profile.getChatid()),
                        new Button("👋 Skip", CALLBACK_SKIP_PREFIX + profile.getChatid()),
                        new Button("🚫 Reportar", CALLBACK_REPORT_PREFIX + profile.getChatid())));
    }

    private void deletePreviousProfileMessage(User user) {
        if (user.getPreviousProfileMessageId() != null) {
            sender.deleteMessage(user.getChatid(), user.getPreviousProfileMessageId());
        }
    }

    private void deleteCurrentProfileMessage(User user) {
        if (user.getCurrentProfileMessageId() != null) {
            sender.deleteMessage(user.getChatid(), user.getCurrentProfileMessageId());
        }
    }

    private void trackCurrentMessageId(User user, MessageUpdate update) {
        if (update.getMessageId() != null) {
            user.setCurrentProfileMessageId(update.getMessageId());
        }
    }

    private String extractTargetChatid(String callback, String prefix) {
        return callback.substring(prefix.length());
    }

    private void sendLikeMessage(String fromChatid, String toChatid) {
        LikeMessage message = new LikeMessage(fromChatid, toChatid);
        jmsTemplate.convertAndSend("queue.like", message);
    }

    private void sendAnalytics(String chatid, String eventType, int increment) {
        AnalyticsMessage message = new AnalyticsMessage();
        message.setChatid(chatid);
        message.setDate(FechaActual.obtenerFechaActual());
        message.setEventType(eventType);
        message.setIncrement(increment);
        jmsTemplate.convertAndSend("queue.analytics", message);
    }

    private void saveReport(String reporterChatid, String reportedChatid) {
        Report report = new Report();
        report.setReporterChatid(reporterChatid);
        report.setReportedChatid(reportedChatid);
        report.setReason(REPORT_REASON_PROFILE);
        report.setStatus(REPORT_STATUS_OPEN);
        report.setCreatedAt(isoTimestamp());
        reportRepository.save(report);
    }

    private void saveSkip(String fromChatid, String toChatid) {
        SkippedProfile skippedProfile = skippedProfileRepository.findByFromChatidAndToChatid(fromChatid, toChatid);
        if (skippedProfile == null) {
            skippedProfile = new SkippedProfile();
            skippedProfile.setFromChatid(fromChatid);
            skippedProfile.setToChatid(toChatid);
        }
        skippedProfile.setExpiresAt(Instant.now().plusSeconds(SKIP_COOLDOWN_DAYS * 24L * 60L * 60L).toString());
        skippedProfileRepository.save(skippedProfile);
    }

    private void notifyAdminAboutReport(Profile reportedProfile) {
        ModerationMessage message = new ModerationMessage();
        message.setType(MODERATION_TYPE_REPORT);
        message.setChatid(reportedProfile == null ? null : reportedProfile.getChatid());
        message.setReason(REPORT_REASON_PROFILE);
        if (reportedProfile != null) {
            message.setName(reportedProfile.getName());
            message.setBirthDate(reportedProfile.getBirthDate() != null ? reportedProfile.getBirthDate().toString() : null);
            message.setGender(reportedProfile.getGender());
            message.setOrientation(reportedProfile.getOrientation());
            message.setCountry(reportedProfile.getCountry());
            message.setCity(reportedProfile.getCity());
            message.setDescription(reportedProfile.getDescription());
            message.setTastes(reportedProfile.getTastes());
            message.setTraits(reportedProfile.getTraits());
            message.setLookingFor(reportedProfile.getLookingFor());
            message.setPhotoFileId(reportedProfile.getPhotoFileId());
            message.setContactUsername(reportedProfile.getContactUsername());
        }
        jmsTemplate.convertAndSend("queue.moderation", message);
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

    private String noProfilesNotice() {
        return "No hay más personas por ahora.\n\nActualizado: " + LocalTime.now().format(NOTICE_TIME_FORMAT);
    }

}
