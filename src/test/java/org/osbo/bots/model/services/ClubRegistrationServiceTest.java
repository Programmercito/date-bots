package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.osbo.bots.util.AgeCalculator;
import org.osbo.bots.util.LookingForOption;
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
    void shouldPauseAndReactivateAnApprovedProfile() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_APPROVED);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        assertThat(service.handleProfileAvailability(user, newUpdate("/pausar_perfil", USERNAME))).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_PAUSED);
        assertThat(profile.getUpdatedAt()).isNotBlank();
        assertThat(user.getComando()).isEqualTo("start");
        verify(profileRepository).save(profile);
        verify(sender).sendMarkdown(eq(CHATID), argThat(text -> text.contains("perfil está pausado")),
                eq(true), argThat(rows -> rows.stream().flatMap(List::stream)
                        .anyMatch(button -> "/activar_perfil".equals(button.getCallbackData()))));

        assertThat(service.handleProfileAvailability(user, newUpdate("/activar_perfil", USERNAME))).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_APPROVED);
        verify(profileRepository, org.mockito.Mockito.times(2)).save(profile);
        verify(sender).sendMarkdown(eq(CHATID), argThat(text -> text.contains("está aprobado")),
                eq(true), argThat(rows -> rows.stream().flatMap(List::stream)
                        .anyMatch(button -> "/pausar_perfil".equals(button.getCallbackData()))));
    }

    @ParameterizedTest
    @MethodSource("unavailableProfileStates")
    void shouldNotChangeIneligibleOrAlreadySetProfiles(String command, String status, String message) {
        User user = newUser("club_edit_name");
        Profile profile = newIncompleteProfile();
        profile.setStatus(status);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        assertThat(service.handleProfileAvailability(user, newUpdate(command, USERNAME))).isTrue();
        assertThat(profile.getStatus()).isEqualTo(status);
        assertThat(user.getComando()).isEqualTo("club_edit_name");
        verify(profileRepository, never()).save(any());
        verify(sender).send(CHATID, message);
    }

    private static Stream<Arguments> unavailableProfileStates() {
        String ineligible = "Necesitás tener un perfil aprobado para cambiar su disponibilidad. Entrá al club con /club.";
        return Stream.of(
                Arguments.of("/pausar_perfil", "PAUSED", "Tu perfil ya está pausado."),
                Arguments.of("/activar_perfil", "APPROVED", "Tu perfil ya está activo."),
                Arguments.of("/pausar_perfil", "PENDING", ineligible),
                Arguments.of("/activar_perfil", "PENDING", ineligible),
                Arguments.of("/pausar_perfil", "INCOMPLETE", ineligible),
                Arguments.of("/activar_perfil", "INCOMPLETE", ineligible),
                Arguments.of("/pausar_perfil", "REJECTED", ineligible),
                Arguments.of("/activar_perfil", "REJECTED", ineligible),
                Arguments.of("/pausar_perfil", null, ineligible),
                Arguments.of("/activar_perfil", null, ineligible));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/pausar_perfil", "/activar_perfil"})
    void shouldExplainAvailabilityRequiresAProfile(String command) {
        User user = newUser("start");
        when(profileRepository.findByChatid(CHATID)).thenReturn(null);

        assertThat(service.handleProfileAvailability(user, newUpdate(command, USERNAME))).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(CHATID), argThat(text -> text.contains("perfil aprobado")));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreOtherCommandsAndNonTextUpdates() {
        User user = newUser("start");

        assertThat(service.handleProfileAvailability(user, newUpdate("/club", USERNAME))).isFalse();
        assertThat(service.handleProfileAvailability(user, newUpdate(null, USERNAME))).isFalse();
        verify(profileRepository, never()).findByChatid(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldDiscardStagedPhotosWhenPausingDuringPhotoEdit() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_PHOTO);
        user.setTempPhotoFileIds("staged-one|staged-two");
        user.setPhotoEditPromptMessageId(123);
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_APPROVED);
        profile.setPhotoFileId("original");
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handleProfileAvailability(user, newUpdate("/pausar_perfil", USERNAME));

        assertThat(user.getTempPhotoFileIds()).isNull();
        assertThat(user.getPhotoEditPromptMessageId()).isNull();
        assertThat(profile.getPhotoFileId()).isEqualTo("original");
        assertThat(user.getComando()).isEqualTo("start");
    }

    @Test
    void shouldPauseWhenUserHasNoCurrentState() {
        User user = newUser(null);
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_APPROVED);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        assertThat(service.handleProfileAvailability(user, newUpdate("/pausar_perfil", USERNAME))).isTrue();

        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_PAUSED);
        assertThat(user.getComando()).isEqualTo("start");
        verify(profileRepository).save(profile);
    }

    @Test
    void shouldReactivateWithoutAnEmptyTelegramHandle() {
        User user = newUser("start");
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_PAUSED);
        profile.setContactUsername(null);
        profile.setWhatsapp("+59170012345");
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handleProfileAvailability(user, newUpdate("/activar_perfil", USERNAME));

        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_APPROVED);
        verify(sender).sendMarkdown(eq(CHATID),
                argThat(text -> text.contains("Tu perfil está aprobado") && !text.contains("@")),
                eq(true), any());
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
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);

        service.handle(user, newUpdate("15/03/2000", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_GENDER);
        assertThat(profile.getBirthDate()).isEqualTo(java.time.LocalDate.of(2000, 3, 15));

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_GENDER_MALE, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_ORIENTATION);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_ORIENTATION_HETERO, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_CITY);
        assertThat(profile.getGender()).isEqualTo(ClubRegistrationService.GENDER_MALE);
        assertThat(profile.getOrientation()).isEqualTo(ClubRegistrationService.ORIENTATION_HETERO);

        service.handle(user, newUpdate(org.osbo.bots.util.CityOption.CALLBACK_CITY_SANTA_CRUZ, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_DESCRIPTION);

        service.handle(user, newUpdate("Friendly and outgoing", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_TASTES);

        service.handle(user, newUpdate("Music, hiking", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_TRAITS);

        service.handle(user, newUpdate("Honest, funny", USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);

        service.handle(user, newUpdate(LookingForOption.CALLBACK_LOOKING_FOR_FRIENDSHIP, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PHOTO);
        assertThat(profile.getLookingFor()).isEqualTo(LookingForOption.LOOKING_FOR_FRIENDSHIP);

        MessageUpdate photoUpdate = newUpdate(null, USERNAME);
        photoUpdate.setMedias(new String[] { "photo-file-id" });
        service.handle(user, photoUpdate);
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PHOTO);
        assertThat(profile.getPhotoFileId()).isEqualTo("photo-file-id");

        // Tap "Listo" to proceed
        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_PHOTO_DONE, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_CONTACT);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_CONTACT_TELEGRAM, USERNAME));
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PREVIEW);
        assertThat(profile.getContactUsername()).isEqualTo(USERNAME);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_PREVIEW_OK, USERNAME));
        assertThat(user.getComando()).isEqualTo("start");
        assertThat(profile.getStatus()).isEqualTo(ClubRegistrationService.STATUS_PENDING);

        ArgumentCaptor<ModerationMessage> moderationCaptor = ArgumentCaptor.forClass(ModerationMessage.class);
        verify(jmsTemplate).convertAndSend(eq("queue.moderation"), moderationCaptor.capture());
        assertThat(moderationCaptor.getValue().getBirthDate()).isEqualTo("2000-03-15");

        ArgumentCaptor<UserPlan> planCaptor = ArgumentCaptor.forClass(UserPlan.class);
        verify(userPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getPlan()).isEqualTo(ClubRegistrationService.PLAN_FREE);
    }

    @Test
    void shouldRejectBirthdateBelowMinimumAge() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("15/03/2010", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidBirthdateFormatAndShowExample() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("15-03-2000", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);
        verify(profileRepository, never()).save(any());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(CHATID), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("DD/MM/AAAA", "15/03/2000");
    }

    @Test
    void shouldAcceptBirthdateOn18thBirthday() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_BIRTHDATE);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, newUpdate("26/07/2008", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_GENDER);
        assertThat(profile.getBirthDate()).isEqualTo(java.time.LocalDate.of(2008, 7, 26));
    }

    @Test
    void shouldRejectInvalidLookingForCallbackAndResendButtons() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("invalid_callback", USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        verify(sender).sendMarkdown(eq(CHATID), any(), eq(true), any(List.class));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldTranslateLookingForInPreview() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_LOOKING_FOR);
        Profile profile = newIncompleteProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate(LookingForOption.CALLBACK_LOOKING_FOR_LOVERS, USERNAME));

        assertThat(profile.getLookingFor()).isEqualTo(LookingForOption.LOOKING_FOR_LOVERS);
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
        verify(sender).sendMarkdown(eq(CHATID), any(String.class), eq(true), any(List.class));
    }

    @Test
    void shouldHandleClubCommandFromAnyState() {
        User user = newUser("publicar");
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_APPROVED);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, newUpdate("/club", USERNAME));

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("publicar");
        verify(sender).sendMarkdown(eq(CHATID), any(String.class), eq(true), any(List.class));
    }

    @Test
    void shouldRestartIncompleteRegistrationWhenClubCommandReceived() {
        User user = newUser("publicar");
        Profile profile = newIncompleteProfile();
        profile.setStatus(ClubRegistrationService.STATUS_INCOMPLETE);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(user, newUpdate("/club", USERNAME));

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_NAME);
        verify(profileRepository).delete(profile);
        verify(profileRepository).save(any());
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

    @Test
    void shouldAcceptValidWhatsappNumberAtContactStep() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_CONTACT);
        Profile profile = newCompleteProfile();
        profile.setContactUsername(null);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, newUpdate("+591 700-12345", null));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_PREVIEW);
        assertThat(profile.getWhatsapp()).isEqualTo("+591 700-12345");
    }

    @Test
    void shouldRejectInvalidWhatsappNumberAndStayInContactStep() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_CONTACT);
        Profile profile = newCompleteProfile();
        profile.setContactUsername(null);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate("llámame", null));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_CONTACT);
        assertThat(profile.getWhatsapp()).isNull();
        verify(sender).send(eq(CHATID), any());
    }

    @Test
    void shouldRejectContactStepWhenNoContactMethodAvailable() {
        User user = newUser(ClubRegistrationService.STATE_REGISTER_CONTACT);
        Profile profile = newCompleteProfile();
        profile.setContactUsername(null);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, newUpdate(ClubRegistrationService.CALLBACK_CONTACT_SKIP, null));

        assertThat(user.getComando()).isEqualTo(ClubRegistrationService.STATE_REGISTER_CONTACT);
        verify(sender).send(eq(CHATID), any());
    }

    private Profile newIncompleteProfile() {
        Profile profile = new Profile();
        profile.setChatid(CHATID);
        profile.setContactUsername(USERNAME);
        profile.setCountry(ClubRegistrationService.COUNTRY_BOLIVIA);
        profile.setStatus(ClubRegistrationService.STATUS_INCOMPLETE);
        return profile;
    }

    private Profile newCompleteProfile() {
        Profile profile = newIncompleteProfile();
        profile.setName("Test Name");
        profile.setBirthDate(java.time.LocalDate.of(2000, 3, 15));
        profile.setGender(ClubRegistrationService.GENDER_MALE);
        profile.setOrientation(ClubRegistrationService.ORIENTATION_HETERO);
        profile.setCity("Santa Cruz");
        profile.setDescription("Friendly");
        profile.setTastes("Music");
        profile.setTraits("Honest");
        profile.setLookingFor(LookingForOption.LOOKING_FOR_FRIENDSHIP);
        profile.setPhotoFileId("photo-id");
        return profile;
    }

}
