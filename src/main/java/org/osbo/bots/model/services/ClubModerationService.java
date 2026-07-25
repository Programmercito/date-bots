package org.osbo.bots.model.services;

import java.time.OffsetDateTime;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles admin moderation actions for friendship club profiles.
 */
@Slf4j
@Component
public class ClubModerationService {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String USER_STATUS_BLOCKED = "bloqueado";

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final NqueueForSend sender;
    private final String adminChatid;

    public ClubModerationService(ProfileRepository profileRepository, UserRepository userRepository,
            NqueueForSend sender, @Value("${telegram.admin}") String adminChatid) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.sender = sender;
        this.adminChatid = adminChatid;
    }

    /**
     * Approves a profile and notifies the user.
     *
     * @param callerChatid the chat ID of the admin performing the action
     * @param targetChatid the chat ID of the profile owner
     * @return true if the action was authorized and executed
     */
    public boolean approveProfile(@NonNull String callerChatid, @NonNull String targetChatid) {
        if (!isAdmin(callerChatid)) {
            return false;
        }
        Profile profile = profileRepository.findByChatid(targetChatid);
        if (profile == null) {
            log.warn("Approve profile failed: profile not found for chatid={}", targetChatid);
            sender.send(callerChatid, "No se encontró el perfil.");
            return false;
        }
        profile.setStatus(STATUS_APPROVED);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);
        sender.send(targetChatid,
                "Tu perfil fue aprobado. Usá /ver_personas para empezar a descubrir personas.");
        sender.send(callerChatid, "Perfil aprobado.");
        return true;
    }

    /**
     * Rejects a profile and notifies the user.
     *
     * @param callerChatid the chat ID of the admin performing the action
     * @param targetChatid the chat ID of the profile owner
     * @return true if the action was authorized and executed
     */
    public boolean rejectProfile(@NonNull String callerChatid, @NonNull String targetChatid) {
        if (!isAdmin(callerChatid)) {
            return false;
        }
        Profile profile = profileRepository.findByChatid(targetChatid);
        if (profile == null) {
            log.warn("Reject profile failed: profile not found for chatid={}", targetChatid);
            sender.send(callerChatid, "No se encontró el perfil.");
            return false;
        }
        profile.setStatus(STATUS_REJECTED);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);
        sender.send(targetChatid, "Tu perfil fue rechazado. Podés volver a registrarte con /club.");
        sender.send(callerChatid, "Perfil rechazado.");
        return true;
    }

    /**
     * Blocks a user and notifies them.
     *
     * @param callerChatid the chat ID of the admin performing the action
     * @param targetChatid the chat ID of the user to block
     * @return true if the action was authorized and executed
     */
    public boolean blockUser(@NonNull String callerChatid, @NonNull String targetChatid) {
        if (!isAdmin(callerChatid)) {
            return false;
        }
        User user = userRepository.findById(targetChatid).orElse(null);
        if (user == null) {
            log.warn("Block user failed: user not found for chatid={}", targetChatid);
            sender.send(callerChatid, "No se encontró el usuario.");
            return false;
        }
        user.setEstado(USER_STATUS_BLOCKED);
        userRepository.save(user);
        sender.send(targetChatid, "Tu cuenta fue bloqueada.");
        sender.send(callerChatid, "Usuario bloqueado.");
        return true;
    }

    private boolean isAdmin(String chatid) {
        return adminChatid.equals(chatid);
    }

    private String isoTimestamp() {
        return OffsetDateTime.now().toString();
    }

}
