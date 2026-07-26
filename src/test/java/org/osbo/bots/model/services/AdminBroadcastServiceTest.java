package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.model.entity.User;

class AdminBroadcastServiceTest {

    private static final String ADMIN_CHATID = "admin-123";
    private static final String CHANNEL = "@amistadbo";
    private static final String USER_CHATID = "user-456";

    private NqueueForSend sender;
    private UserService userService;
    private AdminBroadcastService service;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        userService = mock(UserService.class);
        service = new AdminBroadcastService(sender, userService, ADMIN_CHATID, CHANNEL);
    }

    @Test
    void shouldIgnoreBroadcastFromNonAdmin() {
        boolean handled = service.handle("user-999", "/enviar_todos Hola a todos");

        assertThat(handled).isFalse();
    }

    @Test
    void shouldBroadcastToAllUsers() {
        User user = new User();
        user.setChatid(USER_CHATID);
        when(userService.findAll()).thenReturn(List.of(user));

        boolean handled = service.handle(ADMIN_CHATID, "/enviar_todos Hola a todos");

        assertThat(handled).isTrue();
        verify(sender).sendMarkdown(USER_CHATID, "Hola a todos", false, null);
    }

    @Test
    void shouldSendMessageToChannel() {
        boolean handled = service.handle(ADMIN_CHATID, "/enviar_canal Mensaje para el canal");

        assertThat(handled).isTrue();
        verify(sender).send(CHANNEL, "Mensaje para el canal", "text", null, null, false, null, null, "Markdown");
    }

    @Test
    void shouldReturnTrueForEmptyBroadcastMessageButNotSend() {
        boolean handled = service.handle(ADMIN_CHATID, "/enviar_todos   ");

        assertThat(handled).isTrue();
        verify(userService, org.mockito.Mockito.never()).findAll();
    }
}
