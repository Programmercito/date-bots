package org.osbo.bots.jms.queue.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.util.AgeCalculator;

class ReceiverForModerationTest {

    private static final String ADMIN_CHATID = "admin-123";
    private static final String USER_CHATID = "user-456";
    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    private NqueueForSend sender;
    private ReceiverForModeration receiver;
    private Clock clock;

    @BeforeEach
    void setUp() {
        sender = org.mockito.Mockito.mock(NqueueForSend.class);
        clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);
        receiver = new ReceiverForModeration(sender, ADMIN_CHATID, clock);
    }

    @Test
    void shouldSendAdminPhotoWithProfileDetailsAndButtons() {
        ModerationMessage message = new ModerationMessage();
        message.setType("NEW_PROFILE");
        message.setChatid(USER_CHATID);
        message.setName("Maria");
        message.setBirthDate("1998-09-12");
        message.setGender("FEMALE");
        message.setOrientation("HETERO");
        message.setCity("La Paz");
        message.setDescription("Friendly");
        message.setTastes("Music, hiking");
        message.setTraits("Honest, funny");
        message.setLookingFor("Friends");
        message.setPhotoFileId("photo-id-123");
        message.setContactUsername("maria_user");

        receiver.receiveModeration(message);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(ADMIN_CHATID), textCaptor.capture(), eq("text"), photoCaptor.capture(),
                eq((String) null), eq(false), buttonsCaptor.capture(), eq((String) null), eq("Markdown"));

        String text = textCaptor.getValue();
        assertThat(text).contains("*Nuevo perfil para moderar*");
        assertThat(text).contains("*👤 Nombre:* Maria");
        assertThat(text).contains("*🎂 Edad:* 27");
        assertThat(text).doesNotContain("1998-09-12");
        assertThat(text).contains("*⚧ Género:* Mujer");
        assertThat(text).contains("*💕 Orientación:* Hetero");
        assertThat(text).contains("*📍 Ciudad:* La Paz");
        assertThat(text).contains("*📝 Sobre:* Friendly");
        assertThat(text).contains("*🎸 Gustos:* Music, hiking");
        assertThat(text).contains("*🧠 Personalidad:* Honest, funny");
        assertThat(text).contains("*💘 Buscando:* Friends");
        assertThat(text).contains("*📞 Contacto:* @maria\\_user");
        assertThat(photoCaptor.getValue()).isEqualTo("photo-id-123");

        List<List<Button>> buttons = buttonsCaptor.getValue();
        assertThat(buttons).hasSize(3);
        assertThat(buttons.get(0).get(0).getText()).isEqualTo("✅ Aprobar");
        assertThat(buttons.get(0).get(0).getCallbackData()).isEqualTo("/aprobar_perfil_" + USER_CHATID);
        assertThat(buttons.get(1).get(0).getText()).isEqualTo("❌ Rechazar");
        assertThat(buttons.get(1).get(0).getCallbackData()).isEqualTo("/rechazar_perfil_" + USER_CHATID);
        assertThat(buttons.get(2).get(0).getText()).isEqualTo("⛔ Bloquear usuario");
        assertThat(buttons.get(2).get(0).getCallbackData()).isEqualTo("/bloquear_" + USER_CHATID);
    }

    @Test
    void shouldFallbackToLegacyAgeWhenBirthDateIsMissing() {
        ModerationMessage message = new ModerationMessage();
        message.setType("NEW_PROFILE");
        message.setChatid(USER_CHATID);
        message.setName("Carlos");
        message.setAge(28);
        message.setGender("MALE");
        message.setOrientation("HETERO");
        message.setCity("Santa Cruz");
        message.setDescription("Friendly");
        message.setPhotoFileId("photo-id-456");

        receiver.receiveModeration(message);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(ADMIN_CHATID), textCaptor.capture(), eq("text"), photoCaptor.capture(),
                eq((String) null), eq(false), buttonsCaptor.capture(), eq((String) null), eq("Markdown"));
        assertThat(textCaptor.getValue()).contains("*🎂 Edad:* 28");
        assertThat(photoCaptor.getValue()).isEqualTo("photo-id-456");
    }

    @Test
    void shouldIgnoreInvalidBirthDateAndFallbackToAge() {
        ModerationMessage message = new ModerationMessage();
        message.setType("NEW_PROFILE");
        message.setChatid(USER_CHATID);
        message.setName("Ana");
        message.setBirthDate("not-a-date");
        message.setAge(30);
        message.setGender("FEMALE");
        message.setOrientation("HETERO");
        message.setCity("Cochabamba");
        message.setDescription("Friendly");
        message.setPhotoFileId("photo-id-789");

        receiver.receiveModeration(message);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(ADMIN_CHATID), textCaptor.capture(), eq("text"), photoCaptor.capture(),
                eq((String) null), eq(false), buttonsCaptor.capture(), eq((String) null), eq("Markdown"));
        assertThat(textCaptor.getValue()).contains("*🎂 Edad:* 30");
        assertThat(photoCaptor.getValue()).isEqualTo("photo-id-789");
    }

    @Test
    void shouldIgnoreMessageWithoutChatid() {
        ModerationMessage message = new ModerationMessage();
        message.setType("NEW_PROFILE");
        message.setChatid(null);

        receiver.receiveModeration(message);

        org.mockito.Mockito.verifyNoInteractions(sender);
    }

}
