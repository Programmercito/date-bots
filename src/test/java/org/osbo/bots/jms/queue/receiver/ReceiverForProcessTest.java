package org.osbo.bots.jms.queue.receiver;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.springframework.test.util.ReflectionTestUtils;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.services.ClubDiscoveryService;
import org.osbo.bots.model.services.ClubModerationService;
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
    private LikeMatchService likeMatchService;
    private ReceiverForProcess receiver;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        userService = mock(UserService.class);
        messageService = mock(MessageService.class);
        clubRegistrationService = mock(ClubRegistrationService.class);
        clubModerationService = mock(ClubModerationService.class);
        clubDiscoveryService = mock(ClubDiscoveryService.class);
        likeMatchService = mock(LikeMatchService.class);
        receiver = new ReceiverForProcess(sender, userService, messageService, clubRegistrationService,
                clubModerationService, clubDiscoveryService, likeMatchService);
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
