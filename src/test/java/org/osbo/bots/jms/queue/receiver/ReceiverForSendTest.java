package org.osbo.bots.jms.queue.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageSend;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.model.services.UserService;
import org.springframework.test.util.ReflectionTestUtils;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;

class ReceiverForSendTest {

    private static final String VIEWER_CHATID = "viewer-123";
    private static final String TARGET_CHATID = "target-456";

    private MessageService messageService;
    private UserService userService;
    private ProfileRepository profileRepository;
    private TelegramBot bot;
    private ReceiverForSend receiver;

    @BeforeEach
    void setUp() {
        messageService = mock(MessageService.class);
        userService = mock(UserService.class);
        profileRepository = mock(ProfileRepository.class);
        receiver = new ReceiverForSend(messageService, userService, profileRepository);
        bot = mock(TelegramBot.class);
        receiver.setTelegramBotForTest(bot);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldDecorateChannelPostAndPreserveOriginalText(boolean photo) {
        String original = "Hola *amigos* 😊\nBusco amistades";
        ReflectionTestUtils.setField(receiver, "chatidchannel", "@canal");
        SendResponse response = mock(SendResponse.class);
        com.pengrad.telegrambot.model.Message sent = mock(com.pengrad.telegrambot.model.Message.class);
        when(response.isOk()).thenReturn(true);
        when(response.message()).thenReturn(sent);
        when(sent.messageId()).thenReturn(123);
        if (photo) {
            when(bot.execute(any(SendPhoto.class))).thenReturn(response);
        } else {
            when(bot.execute(any(SendMessage.class))).thenReturn(response);
        }
        MessageSend message = new MessageSend();
        message.setChatid(VIEWER_CHATID);
        message.setTipo("channel");
        message.setText(original);
        message.setButtons(List.of(List.of(new Button("✉️ Escríbele", null, "https://t.me/usuario"))));
        if (photo) {
            message.setMedias(new String[] { "photo-id" });
        }

        receiver.sendMessage(message);

        if (photo) {
            ArgumentCaptor<SendPhoto> request = ArgumentCaptor.forClass(SendPhoto.class);
            verify(bot).execute(request.capture());
            assertThat((String) request.getValue().getParameters().get("caption"))
                    .contains("💌 Una nueva conexión", original);
            assertThat(request.getValue().getParameters()).doesNotContainKey("parse_mode");
        } else {
            ArgumentCaptor<SendMessage> request = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot).execute(request.capture());
            assertThat((String) request.getValue().getParameters().get("text"))
                    .contains("💌 Una nueva conexión", original);
            assertThat(request.getValue().getParameters()).doesNotContainKey("parse_mode");
        }
        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messageService).save(saved.capture());
        assertThat(saved.getValue().getTexto()).isEqualTo(original);
    }

    @Test
    void shouldDecorateApprovedPostWithoutChangingStoredSubmission() {
        ReflectionTestUtils.setField(receiver, "chatidchannel", "@canal");
        Message stored = new Message();
        stored.setTexto("Mensaje original *sin cambios*");
        when(messageService.findById("approval-id")).thenReturn(stored);
        SendResponse response = mock(SendResponse.class);
        com.pengrad.telegrambot.model.Message sent = mock(com.pengrad.telegrambot.model.Message.class);
        when(response.isOk()).thenReturn(true);
        when(response.message()).thenReturn(sent);
        when(sent.messageId()).thenReturn(321);
        when(bot.execute(any(SendPhoto.class))).thenReturn(response);
        MessageSend message = new MessageSend();
        message.setChatid("author");
        message.setTipo("channel");
        message.setText(stored.getTexto());
        message.setMsgid("approval-id");
        message.setMedias(new String[] { "photo-id" });

        receiver.sendMessage(message);

        ArgumentCaptor<SendPhoto> request = ArgumentCaptor.forClass(SendPhoto.class);
        verify(bot).execute(request.capture());
        assertThat((String) request.getValue().getParameters().get("caption"))
                .contains("💌 Una nueva conexión", "Mensaje original *sin cambios*");
        assertThat(stored.getTexto()).isEqualTo("Mensaje original *sin cambios*");
        assertThat(stored.getEstado()).isEqualTo("publicado");
        assertThat(stored.getMessageid()).isEqualTo("321");
        verify(messageService).save(stored);
    }

    @Test
    void shouldDeactivateProfileAndNotifyOwnerWhenDiscoveryPhotoFails() {
        Profile profile = new Profile();
        profile.setChatid(TARGET_CHATID);
        profile.setStatus("APPROVED");
        when(profileRepository.findByChatid(TARGET_CHATID)).thenReturn(profile);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isOk()).thenReturn(false);
        when(bot.execute(any(SendPhoto.class))).thenReturn(failedResponse);

        MessageSend message = new MessageSend();
        message.setChatid(VIEWER_CHATID);
        message.setTipo("discovery_profile");
        message.setText("Test profile");
        message.setMedias(new String[] { "broken-photo-id" });
        message.setTargetProfileChatid(TARGET_CHATID);
        message.setButtons(List.of(List.of(new Button("Like", "club_like_" + TARGET_CHATID))));

        receiver.sendMessage(message);

        assertThat(profile.getStatus()).isEqualTo("REJECTED");
        assertThat(profile.getUpdatedAt()).isNotNull();
        verify(profileRepository).save(profile);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, org.mockito.Mockito.times(2)).execute(messageCaptor.capture());
        List<SendMessage> sentMessages = messageCaptor.getAllValues();
        SendMessage ownerMessage = sentMessages.stream()
                .filter(m -> TARGET_CHATID.equals(m.getParameters().get("chat_id")))
                .findFirst()
                .orElseThrow();
        assertThat((String) ownerMessage.getParameters().get("text"))
                .contains("foto de perfil no pudo enviarse");
    }

    @Test
    void shouldUpdateCurrentProfileMessageIdOnSuccessfulDiscoveryPhoto() {
        User viewer = new User();
        viewer.setChatid(VIEWER_CHATID);
        when(userService.findById(VIEWER_CHATID)).thenReturn(viewer);

        SendResponse okResponse = mock(SendResponse.class);
        when(okResponse.isOk()).thenReturn(true);
        com.pengrad.telegrambot.model.Message sentMessage = mock(com.pengrad.telegrambot.model.Message.class);
        when(sentMessage.messageId()).thenReturn(42);
        when(okResponse.message()).thenReturn(sentMessage);
        when(bot.execute(any(SendPhoto.class))).thenReturn(okResponse);

        MessageSend message = new MessageSend();
        message.setChatid(VIEWER_CHATID);
        message.setTipo("discovery_profile");
        message.setText("Test profile");
        message.setMedias(new String[] { "valid-photo-id" });
        message.setTargetProfileChatid(TARGET_CHATID);

        receiver.sendMessage(message);

        assertThat(viewer.getCurrentProfileMessageId()).isEqualTo(42);
        verify(userService).save(viewer);
    }

    @Test
    void shouldUpdateCurrentProfileMessageIdOnSuccessfulEmptyDiscoveryNotice() {
        User viewer = new User();
        viewer.setChatid(VIEWER_CHATID);
        when(userService.findById(VIEWER_CHATID)).thenReturn(viewer);

        SendResponse okResponse = mock(SendResponse.class);
        when(okResponse.isOk()).thenReturn(true);
        com.pengrad.telegrambot.model.Message sentMessage = mock(com.pengrad.telegrambot.model.Message.class);
        when(sentMessage.messageId()).thenReturn(43);
        when(okResponse.message()).thenReturn(sentMessage);
        when(bot.execute(any(SendMessage.class))).thenReturn(okResponse);

        MessageSend message = new MessageSend();
        message.setChatid(VIEWER_CHATID);
        message.setTipo("discovery_empty");
        message.setText("No hay más personas por ahora.\n\nActualizado: 23:00:00");

        receiver.sendMessage(message);

        assertThat(viewer.getCurrentProfileMessageId()).isEqualTo(43);
        verify(userService).save(viewer);
    }

    @Test
    void shouldFallBackToTextWhenMatchNotificationPhotoFails() {
        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isOk()).thenReturn(false);
        when(bot.execute(any(SendPhoto.class))).thenReturn(failedResponse);

        SendResponse okResponse = mock(SendResponse.class);
        when(okResponse.isOk()).thenReturn(true);
        when(bot.execute(any(SendMessage.class))).thenReturn(okResponse);

        MessageSend message = new MessageSend();
        message.setChatid(VIEWER_CHATID);
        message.setTipo("match_notification");
        message.setText("Match caption");
        message.setMedias(new String[] { "broken-match-photo-id" });
        message.setButtons(List.of(List.of(new Button("Report", "club_match_report_" + TARGET_CHATID))));

        receiver.sendMessage(message);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, org.mockito.Mockito.times(1)).execute(any(SendPhoto.class));
        verify(bot, org.mockito.Mockito.times(1)).execute(messageCaptor.capture());
        SendMessage fallback = messageCaptor.getValue();
        assertThat((String) fallback.getParameters().get("chat_id")).isEqualTo(VIEWER_CHATID);
        assertThat((String) fallback.getParameters().get("text")).isEqualTo("Match caption");
    }

    @Test
    void shouldFallBackToTextWhenModerationPhotoFails() {
        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isOk()).thenReturn(false);
        when(bot.execute(any(SendPhoto.class))).thenReturn(failedResponse);

        SendResponse okResponse = mock(SendResponse.class);
        when(okResponse.isOk()).thenReturn(true);
        when(bot.execute(any(SendMessage.class))).thenReturn(okResponse);

        MessageSend message = new MessageSend();
        message.setChatid("admin-123");
        message.setTipo("text");
        message.setText("Moderation caption");
        message.setMedias(new String[] { "broken-moderation-photo-id" });
        message.setButtons(List.of(List.of(new Button("Aprobar", "/aprobar_perfil_123"))));

        receiver.sendMessage(message);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, org.mockito.Mockito.times(1)).execute(any(SendPhoto.class));
        verify(bot, org.mockito.Mockito.times(1)).execute(messageCaptor.capture());
        SendMessage fallback = messageCaptor.getValue();
        assertThat((String) fallback.getParameters().get("chat_id")).isEqualTo("admin-123");
        assertThat((String) fallback.getParameters().get("text")).isEqualTo("Moderation caption");
    }
}
