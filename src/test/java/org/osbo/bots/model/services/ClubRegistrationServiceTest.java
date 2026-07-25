package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.entity.UserPlan;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserPlanRepository;
import org.springframework.jms.core.JmsTemplate;

class ClubRegistrationServiceTest {

    private NqueueForSend sender;
    private ProfileRepository profileRepository;
    private UserPlanRepository userPlanRepository;
    private JmsTemplate jmsTemplate;
    private ClubRegistrationService service;

    private static final String CHATID = "123456";
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        profileRepository = mock(ProfileRepository.class);
        userPlanRepository = mock(UserPlanRepository.class);
        jmsTemplate = mock(JmsTemplate.class);
        service = new ClubRegistrationService(sender, profileRepository, userPlanRepository, jmsTemplate);
    }

    @Test
    void shouldRejectClubWithoutUsername() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/club", null);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(CHATID), any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldStartRegistrationWhenNoProfileExists() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/club", USERNAME);
        when(profileRepository.findByChatid(CHATID)).thenReturn(null);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_NAME);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        Profile saved = profileCaptor.getValue();
        assertThat(saved.getChatid()).isEqualTo(CHATID);
        assertThat(saved.getContactUsername()).isEqualTo(USERNAME);
        assertThat(saved.getCountry()).isEqualTo(ClubRegistrationService.COUNTRY_BOLIVIA);
        assertThat(saved.getStatus()).isEqualTo(ClubRegistrationService.STATUS_INCOMPLETE);
    }

    @Test
    void shouldCollectAllRegistrationFieldsAndSubmitProfile() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_NAME);
        MessageUpdate nameUpdate = newUpdate("Test Name", USERNAME);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userPlanRepository.existsById(CHATID)).thenReturn(false);
        when(userPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, nameUpdate);
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_AGE);

        service.handle(user, newUpdate("25", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_GENDER);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_GENDER_MALE, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_ORIENTATION);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_ORIENTATION_HETERO, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_CITY);
        assertThat(profile.getGender()).isEqualTo(ClubRegistrationService.GENDER_MALE);
        assertThat(profile.getOrientation()).isEqualTo(ClubRegistrationService.ORIENTATION_HETERO);

        service.handle(user, newUpdate("Santa Cruz", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_DESCRIPTION);

        service.handle(user, newUpdate("Friendly and outgoing", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_TASTES);

        service.handle(user, newUpdate("Music, hiking", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_TRAITS);

        service.handle(user, newUpdate("Honest, funny", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_LOOKING_FOR_FRIENDSHIP, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PHOTO);
        assertThat(profile.getLookingFor()).isEqualTo(ClubRegistrationService.LOOKING_FOR_FRIENDSHIP);

        MessageUpdate photoUpdate = newUpdate(null, USERNAME);
        photoUpdate.setMedias(new String[] { "photo-file-id" });
        service.handle(user, photoUpdate);
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PREVIEW);
        assertThat(profile.getPhotoFileId()).isEqualTo("photo-file-id");

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_PREVIEW_OK, USERNAME));
        assertThat(user.getComando()).isEqualTo("start");
        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_PENDING);

        verify(jmsTemplate).convertAndSend(eq("queue.moderation"), any(ModerationMessage.class));

        ArgumentCaptor<UserPlan> planCaptor = ArgumentCaptor.forClass(UserPlan.class);
        verify(userPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getPlan()).isEqualTo(ClubRegistrationService.PLAN_FREE);
    }

    @Test
    void shouldRejectAgeBelowMinimum() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_AGE);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("17", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_AGE);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidLookingForCallbackAndResendButtons() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("invalid_callback", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        verify(sender).send(eq(CHATID), any(), eq(true), any(List.class));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldTranslateLookingForInPreview() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_LOOKING_FOR_LOVERS, USERNAME));

        assertThat(profile.getLookingFor()).isEqualTo(ClubRegistrationService.LOOKING_FOR_LOVERS);
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PHOTO);
    }

    @Test
    void shouldRejectInvalidGenderOrientationCombination() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_ORIENTATION);
        Profile profile = newIncompleteProfile();
        profile.setGender(ClubRegistrationService.GENDER_MALE);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_ORIENTATION_BI, USERNAME));

        assertThat(user.getComando()).isEqualTo("start");
        verify(profileRepository).delete(profile);
    }

    @Test
    void shouldShowApprovedProfileStatus() {
        User user = newUser("start");
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_APPROVED);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, newUpdate("/club", USERNAME));

        assertThat(handled).isTrue();
        verify(sender).send(eq(CHATID), any(), eq(true), any(List.class));
    }

    @Test
    void shouldAllowReRegistrationAfterRejection() {
        User user = newUser("start");
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_REJECTED);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_CLUB_ENTER, USERNAME));

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_NAME);
        verify(profileRepository).delete(profile);
        verify(profileRepository).save(any());
    }

    private User newUser(String comando) {
        User user = new User();
        user.setChatid(CHATID);
        user.setComando(comando);
        return user;
    }

    private MessageUpdate newUpdate(String text, String user) {
        MessageUpdate update = new MessageUpdate();
        update.setChatid(CHATID);
        update.setText(text);
        update.setUser(user);
        return update;
    }

    private Profile newIncompleteProfile() {
        Profile profile = new Profile();
        profile.setChatid(CHATID);
        profile.setContactUsername(USERNAME);
        profile.setCountry(ClubRegistrationService.COUNTRY_BOLIVIA);
        profile.setStatus(ClubRegistrationService.STATUS_INCOMPLETE);
        return profile;
    }

}
