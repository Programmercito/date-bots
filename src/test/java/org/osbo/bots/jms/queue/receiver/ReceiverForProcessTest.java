package org.osbo.bots.jms.queue.receiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.springframework.test.util.ReflectionTestUtils;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.services.AdminBroadcastService;
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
        receiver = new ReceiverForProcess(sender, userService, messageService, clubRegistrationService,
                clubModerationService, clubDiscoveryService, clubProfileEditService, likeMatchService,
                adminBroadcastService);
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
