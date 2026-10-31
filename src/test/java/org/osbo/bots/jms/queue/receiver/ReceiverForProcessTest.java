package org.osbo.bots.jms.queue.receiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.springframework.test.util.ReflectionTestUtils;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.services.AdminBroadcastService;
import org.osbo.bots.model.services.AdminReceivedLikesService;
import org.osbo.bots.model.services.ClubDiscoveryService;
import org.osbo.bots.model.services.ClubModerationService;
import org.osbo.bots.model.services.ClubProfileEditService;
import org.osbo.bots.model.services.ClubRegistrationService;
import org.osbo.bots.model.services.LikeMatchService;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.model.services.UserService;

class ReceiverForProcessTest {

    private static final String CHATID = "user-123";

    private NqueueForSend sender;
    private UserService userService;
    private MessageService messageService;
    private ClubRegistrationService clubRegistrationService;
    private ClubModerationService clubModerationService;
    private ClubDiscoveryService clubDiscoveryService;
    private ClubProfileEditService clubProfileEditService;
    private LikeMatchService likeMatchService;
    private AdminBroadcastService adminBroadcastService;
    private AdminReceivedLikesService adminReceivedLikesService;
    private ReceiverForProcess receiver;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        userService = mock(UserService.class);
        messageService = mock(MessageService.class);
        clubRegistrationService = mock(ClubRegistrationService.class);
        clubModerationService = mock(ClubModerationService.class);
        clubDiscoveryService = mock(ClubDiscoveryService.class);
        clubProfileEditService = mock(ClubProfileEditService.class);
        likeMatchService = mock(LikeMatchService.class);
        adminBroadcastService = mock(AdminBroadcastService.class);
        adminReceivedLikesService = mock(AdminReceivedLikesService.class);
        receiver = new ReceiverForProcess(sender, userService, messageService, clubRegistrationService,
                clubModerationService, clubDiscoveryService, clubProfileEditService, likeMatchService,
                adminBroadcastService, adminReceivedLikesService);
        ReflectionTestUtils.setField(receiver, "adminid", "admin-999");
    }

    @Test
    void shouldRouteMisMatchesCommandToLikeMatchService() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/mis_matches");
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);

        receiver.sendMessage(update);

        verify(likeMatchService).listMatches(CHATID);
    }

    @Test
    void shouldRouteMatchReportCallbackToLikeMatchService() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("club_match_report_target-456");
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);

        receiver.sendMessage(update);

        verify(likeMatchService).reportMatch(CHATID, "target-456");
    }

    @Test
    void shouldRouteEditProfileCommandToClubProfileEditService() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/editar_perfil");
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);
        when(clubRegistrationService.handle(user, update)).thenReturn(false);
        when(clubProfileEditService.handle(user, update)).thenReturn(true);

        receiver.sendMessage(update);

        verify(clubProfileEditService).handle(user, update);
    }

    @ParameterizedTest
    @CsvSource({
            "/pausar_perfil, club_edit_name",
            "/pausar_perfil, club_browsing",
            "/activar_perfil, club_register_name",
            "/activar_perfil, start"
    })
    void shouldRouteAvailabilityFromAnyStateBeforeEditOrRegistration(String command, String state) {
        User user = newUser(state);
        MessageUpdate update = newUpdate(command);
        when(userService.findById(CHATID)).thenReturn(user);
        when(clubRegistrationService.handleProfileAvailability(user, update)).thenReturn(true);

        receiver.sendMessage(update);

        verify(clubRegistrationService).handleProfileAvailability(user, update);
        verify(clubProfileEditService, org.mockito.Mockito.never()).handle(user, update);
        verify(clubRegistrationService, org.mockito.Mockito.never()).handle(user, update);
        verify(userService).save(user);
    }

    @Test
    void shouldDeleteMatchListMessageAndShowStartMenuOnMatchesBack() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("matches_back");
        update.setMessageId(123);
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);

        receiver.sendMessage(update);

        verify(sender).deleteMessage(CHATID, 123);
        verify(sender).send(eq(CHATID), anyString(), eq(true), any());
    }

    @Test
    void shouldReturnToStartMenuFromAnyStateOnStartCommand() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        user.setCurrentProfileMessageId(100);
        MessageUpdate update = newUpdate("/start");
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);

        receiver.sendMessage(update);

        verify(sender).send(eq(CHATID), anyString(), eq(true), any());
        org.assertj.core.api.Assertions.assertThat(user.getComando()).isEqualTo("start");
    }

    @Test
    void shouldShowStartMenuFromStartStateOnStartCommand() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/start");
        when(userService.findById(CHATID)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);

        receiver.sendMessage(update);

        verify(sender).send(eq(CHATID), anyString(), eq(true), any());
        org.assertj.core.api.Assertions.assertThat(user.getComando()).isEqualTo("start");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldShowReadableMenuWithAllExistingActions() {
        User user = newUser("start");
        when(userService.findById(CHATID)).thenReturn(user);

        receiver.sendMessage(newUpdate("/start"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttons = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(CHATID), text.capture(), eq(true), buttons.capture());
        org.assertj.core.api.Assertions.assertThat(text.getValue()).contains("Bolivia", "canal", "club");
        org.assertj.core.api.Assertions.assertThat(buttons.getValue().stream().flatMap(List::stream)
                .map(Button::getCallbackData))
                .containsExactly("/publicar", "/ver_canal", "/club", "/ver_personas", "/mis_matches", "/editar_perfil");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldShowReceivedLikesButtonOnlyToAdmin() {
        User user = newUser("start");
        user.setChatid("admin-999");
        MessageUpdate update = newUpdate("/start");
        update.setChatid("admin-999");
        when(userService.findById("admin-999")).thenReturn(user);

        receiver.sendMessage(update);

        ArgumentCaptor<List<List<Button>>> buttons = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq("admin-999"), anyString(), eq(true), buttons.capture());
        org.assertj.core.api.Assertions.assertThat(buttons.getValue().stream().flatMap(List::stream)
                .map(Button::getCallbackData)).contains(AdminReceivedLikesService.COMMAND);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/likes_recibidos_admin", "admin_likes_page_1"
    })
    void shouldRouteReceivedLikesBeforeOtherConversationStates(String text) {
        User user = newUser("club_edit_name");
        MessageUpdate update = newUpdate(text);
        when(userService.findById(CHATID)).thenReturn(user);
        when(adminReceivedLikesService.handle(CHATID, text)).thenReturn(true);

        receiver.sendMessage(update);

        verify(adminReceivedLikesService).handle(CHATID, text);
        verify(clubProfileEditService, org.mockito.Mockito.never()).handle(user, update);
        verify(userService).save(user);
        org.assertj.core.api.Assertions.assertThat(user.getComando()).isEqualTo("club_edit_name");
    }

    @Test
    void shouldKeepPhotoWhenPublishingWithoutModeration() {
        User user = newUser("publicar");
        MessageUpdate update = newUpdate("Hola, Bolivia");
        update.setUser("usuario");
        update.setMedias(new String[] { "photo-id" });
        when(userService.findById(CHATID)).thenReturn(user);
        ReflectionTestUtils.setField(receiver, "inicio", "00:00");
        ReflectionTestUtils.setField(receiver, "fin", "00:00");
        ReflectionTestUtils.setField(receiver, "aprob", "false");

        receiver.sendMessage(update);

        verify(sender).sendChannel(CHATID, "Hola, Bolivia", "usuario", "photo-id");
        org.assertj.core.api.Assertions.assertThat(user.getComando()).isEqualTo("start");
    }

    private User newUser(String comando) {
        User user = new User();
        user.setChatid(CHATID);
        user.setComando(comando);
        user.setEstado("activo");
        return user;
    }

    private MessageUpdate newUpdate(String text) {
        MessageUpdate update = new MessageUpdate();
        update.setChatid(CHATID);
        update.setText(text);
        return update;
    }

}
