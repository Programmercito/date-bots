package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.osbo.bots.util.LookingForOption;

class ClubProfileEditServiceTest {

    private static final String CHATID = "user-123";
    private static final String USERNAME = "testuser";

    private NqueueForSend sender;
    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private ClubRegistrationService clubRegistrationService;
    private ClubProfileEditService service;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        profileRepository = mock(ProfileRepository.class);
        userRepository = mock(UserRepository.class);
        clubRegistrationService = mock(ClubRegistrationService.class);
        service = new ClubProfileEditService(sender, profileRepository, userRepository, clubRegistrationService);
    }

    @Test
    void shouldRejectEditWhenNoProfile() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/editar_perfil", USERNAME);
        when(profileRepository.findByChatid(CHATID)).thenReturn(null);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(CHATID), anyString());
    }

    @Test
    void shouldRejectEditWhenProfileNotApproved() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/editar_perfil", USERNAME);
        Profile profile = approvedProfile();
        profile.setStatus(ClubRegistrationService.STATUS_PENDING);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(CHATID), anyString());
    }

    @Test
    void shouldShowEditMenuForApprovedProfile() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/editar_perfil", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldStartEditMenuFromAnyStateForEditCommand() {
        User user = newUser("club_browsing");
        MessageUpdate update = newUpdate("/editar_perfil", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldEditName() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_NAME);
        MessageUpdate update = newUpdate("Nuevo nombre", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getName()).isEqualTo("Nuevo nombre");
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldEditBirthdateAndRecalculateAge() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_BIRTHDATE);
        MessageUpdate update = newUpdate("10/05/1998", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1998, 5, 10));
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldRejectBirthdateBelowMinimumAge() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_BIRTHDATE);
        MessageUpdate update = newUpdate("10/05/2015", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        assertThat(profile.getBirthDate()).isNull();
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_BIRTHDATE);
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void shouldEditCity() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_CITY);
        MessageUpdate update = newUpdate(org.osbo.bots.util.CityOption.CALLBACK_CITY_COCHABAMBA, USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getCity()).isEqualTo("Cochabamba");
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldEditGenderAndKeepApproved() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_GENDER);
        MessageUpdate update = newUpdate(ClubRegistrationService.CALLBACK_GENDER_FEMALE, USERNAME);
        Profile profile = approvedProfile();
        profile.setGender(ClubRegistrationService.GENDER_MALE);
        profile.setOrientation(ClubRegistrationService.ORIENTATION_HETERO);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getGender()).isEqualTo(ClubRegistrationService.GENDER_FEMALE);
        assertThat(profile.getStatus()).isEqualTo(ClubProfileEditService.STATUS_APPROVED);
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldRejectInvalidGenderOrientationCombination() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_GENDER);
        MessageUpdate update = newUpdate(ClubRegistrationService.CALLBACK_GENDER_MALE, USERNAME);
        Profile profile = approvedProfile();
        profile.setGender(ClubRegistrationService.GENDER_FEMALE);
        profile.setOrientation(ClubRegistrationService.ORIENTATION_BI);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getGender()).isEqualTo(ClubRegistrationService.GENDER_MALE);
        assertThat(profile.getStatus()).isEqualTo(ClubProfileEditService.STATUS_REJECTED);
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
    }

    @Test
    void shouldEditLookingFor() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_LOOKING_FOR);
        MessageUpdate update = newUpdate(LookingForOption.CALLBACK_LOOKING_FOR_CASUAL, USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getLookingFor()).isEqualTo(LookingForOption.LOOKING_FOR_CASUAL);
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldResendLookingForButtonsForInvalidCallback() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_LOOKING_FOR);
        MessageUpdate update = newUpdate("invalid_callback", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_LOOKING_FOR);
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
    }

    @Test
    void shouldEditPhoto() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_PHOTO);
        MessageUpdate photoUpdate = newUpdate(null, USERNAME);
        photoUpdate.setMedias(new String[] { "new-photo-id" });
        Profile profile = approvedProfile();
        // Clear existing photos so the first upload triggers the "Listo" button message
        profile.setPhotoFileId(null);
        profile.setPhotoFileIds(null);
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, photoUpdate);

        assertThat(profile.getPhotoFileId()).isEqualTo("new-photo-id");
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_PHOTO);

        // Tap "Listo" to save and return to menu
        service.handle(user, newUpdate(ClubProfileEditService.CALLBACK_EDIT_PHOTO_DONE, USERNAME));

        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("new-photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldEditContactToTelegram() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_CONTACT);
        MessageUpdate update = newUpdate(ClubRegistrationService.CALLBACK_CONTACT_TELEGRAM, USERNAME);
        Profile profile = approvedProfile();
        profile.setWhatsapp("+591 70012345");
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getContactUsername()).isEqualTo(USERNAME);
        assertThat(profile.getWhatsapp()).isEqualTo("+591 70012345");
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldEditContactToWhatsapp() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_CONTACT);
        MessageUpdate update = newUpdate("+591 700-98765", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(profile.getWhatsapp()).isEqualTo("+591 700-98765");
        assertThat(profile.getContactUsername()).isEqualTo(USERNAME);
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
    }

    @Test
    void shouldRejectInvalidWhatsapp() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_CONTACT);
        MessageUpdate update = newUpdate("llámame", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        assertThat(profile.getWhatsapp()).isNull();
        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_CONTACT);
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void shouldFinishEdit() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_MENU);
        MessageUpdate update = newUpdate(ClubProfileEditService.CALLBACK_EDIT_FINISH, USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
    }

    @Test
    void shouldMenuShowCurrentProfileValues() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/editar_perfil", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), textCaptor.capture(), eq(true), buttonsCaptor.capture(),
                eq("Markdown"));

        String text = textCaptor.getValue();
        assertThat(text).contains("*👤 Nombre:*");
        assertThat(text).contains("*📍 Ciudad:*");
        assertThat(text).contains("*💘 Buscás:* Amistad");

        List<List<Button>> buttons = buttonsCaptor.getValue();
        List<String> labels = buttons.stream().flatMap(List::stream).map(Button::getText).toList();
        assertThat(labels).contains("👤 Nombre", "🎂 Fecha de nacimiento", "⚧ Género", "💕 Orientación", "📍 Ciudad",
                "📝 Descripción", "🎸 Gustos", "🧠 Personalidad", "💘 Buscando", "📷 Foto", "📞 Contacto",
                "✅ Terminar", "⬅️ Volver al menú del club", "🏠 Volver al inicio");
    }

    @Test
    void shouldCancelEditWithClubCommand() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_NAME);
        MessageUpdate update = newUpdate("/club", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(clubRegistrationService).sendApprovedStatus(CHATID, profile);
    }

    @Test
    void shouldCancelEditWithStartCommand() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_NAME);
        MessageUpdate update = newUpdate("/start", USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).sendMarkdown(eq(CHATID), anyString(), eq(true), any(List.class));
        verify(clubRegistrationService, never()).sendApprovedStatus(anyString(), any(Profile.class));
    }

    @Test
    void shouldReturnToMenuWhenCancelButtonPressed() {
        User user = newUser(ClubProfileEditService.STATE_EDIT_NAME);
        MessageUpdate update = newUpdate(ClubProfileEditService.CALLBACK_EDIT_CANCEL, USERNAME);
        Profile profile = approvedProfile();
        when(profileRepository.findByChatid(CHATID)).thenReturn(profile);

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo(ClubProfileEditService.STATE_EDIT_MENU);
        verify(sender).sendPhoto(eq(CHATID), eq("photo-id"), anyString(), eq(true), any(List.class), eq("Markdown"));
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

    private Profile approvedProfile() {
        Profile profile = new Profile();
        profile.setChatid(CHATID);
        profile.setName("Test");
        profile.setContactUsername(USERNAME);
        profile.setCountry(ClubRegistrationService.COUNTRY_BOLIVIA);
        profile.setStatus(ClubProfileEditService.STATUS_APPROVED);
        profile.setCity("Santa Cruz");
        profile.setDescription("Friendly");
        profile.setTastes("Music");
        profile.setTraits("Funny");
        profile.setLookingFor(LookingForOption.LOOKING_FOR_FRIENDSHIP);
        profile.setGender(ClubRegistrationService.GENDER_MALE);
        profile.setOrientation(ClubRegistrationService.ORIENTATION_HETERO);
        profile.setPhotoFileId("photo-id");
        return profile;
    }

}
