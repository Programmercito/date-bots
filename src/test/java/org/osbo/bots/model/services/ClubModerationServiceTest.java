package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;

class ClubModerationServiceTest {

    private static final String ADMIN_CHATID = "admin-123";
    private static final String USER_CHATID = "user-456";

    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private NqueueForSend sender;
    private ClubModerationService service;

    @BeforeEach
    void setUp() {
        profileRepository = org.mockito.Mockito.mock(ProfileRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        sender = org.mockito.Mockito.mock(NqueueForSend.class);
        service = new ClubModerationService(profileRepository, userRepository, sender, ADMIN_CHATID);
    }

    @Test
    void shouldApproveProfileAndNotifyUser() {
        Profile profile = pendingProfile();
        when(profileRepository.findByChatid(USER_CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = service.approveProfile(ADMIN_CHATID, USER_CHATID);

        assertThat(result).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ClubModerationService.STATUS_APPROVED);
        assertThat(profile.getUpdatedAt()).isNotNull();
        verify(profileRepository).save(profile);
        verify(sender).send(eq(USER_CHATID), eq("Tu perfil fue aprobado. Usá /ver_personas para empezar a descubrir personas."));
        verify(sender).send(eq(ADMIN_CHATID), eq("Perfil aprobado."));
    }

    @Test
    void shouldRejectProfileAndNotifyUser() {
        Profile profile = pendingProfile();
        when(profileRepository.findByChatid(USER_CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = service.rejectProfile(ADMIN_CHATID, USER_CHATID);

        assertThat(result).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ClubModerationService.STATUS_REJECTED);
        verify(sender).send(eq(USER_CHATID), eq("Tu perfil fue rechazado. Podés volver a registrarte con /club."));
        verify(sender).send(eq(ADMIN_CHATID), eq("Perfil rechazado."));
    }

    @Test
    void shouldBlockUserAndNotifyThem() {
        User user = activeUser();
        when(userRepository.findById(USER_CHATID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = service.blockUser(ADMIN_CHATID, USER_CHATID);

        assertThat(result).isTrue();
        assertThat(user.getEstado()).isEqualTo(ClubModerationService.USER_STATUS_BLOCKED);
        verify(sender).send(eq(USER_CHATID), eq("Tu cuenta fue bloqueada."));
        verify(sender).send(eq(ADMIN_CHATID), eq("Usuario bloqueado."));
    }

    @Test
    void shouldRejectApprovalFromNonAdmin() {
        boolean result = service.approveProfile(USER_CHATID, USER_CHATID);

        assertThat(result).isFalse();
        verify(profileRepository, never()).findByChatid(any());
        verify(sender, never()).send(any(), any());
    }

    @Test
    void shouldNotifyAdminWhenProfileNotFound() {
        when(profileRepository.findByChatid(USER_CHATID)).thenReturn(null);

        boolean result = service.approveProfile(ADMIN_CHATID, USER_CHATID);

        assertThat(result).isFalse();
        verify(sender).send(eq(ADMIN_CHATID), eq("No se encontró el perfil."));
    }

    @Test
    void shouldNotifyAdminWhenUserNotFound() {
        when(userRepository.findById(USER_CHATID)).thenReturn(Optional.empty());

        boolean result = service.blockUser(ADMIN_CHATID, USER_CHATID);

        assertThat(result).isFalse();
        verify(sender).send(eq(ADMIN_CHATID), eq("No se encontró el usuario."));
    }

    @Test
    void shouldCaptureSavedProfileStatus() {
        Profile profile = pendingProfile();
        when(profileRepository.findByChatid(USER_CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveProfile(ADMIN_CHATID, USER_CHATID);

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubModerationService.STATUS_APPROVED);
    }

    private Profile pendingProfile() {
        Profile profile = new Profile();
        profile.setChatid(USER_CHATID);
        profile.setName("Test");
        profile.setStatus("PENDING");
        return profile;
    }

    private User activeUser() {
        User user = new User();
        user.setChatid(USER_CHATID);
        user.setEstado("activo");
        return user;
    }

}
