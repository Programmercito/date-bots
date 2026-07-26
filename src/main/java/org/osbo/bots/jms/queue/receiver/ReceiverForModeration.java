package org.osbo.bots.jms.queue.receiver;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.util.AgeCalculator;
import org.osbo.bots.util.LookingForOption;
import org.osbo.bots.util.MarkdownEscaper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes moderation messages from queue.moderation and forwards them to the
 * admin with approve/reject/block actions.
 */
@Slf4j
@Component
public class ReceiverForModeration {

    private final NqueueForSend sender;
    private final String adminChatid;
    private final Clock clock;

    @Autowired
    ReceiverForModeration(NqueueForSend sender, @Value("${telegram.admin}") String adminChatid) {
        this(sender, adminChatid, Clock.system(ZoneId.of("America/La_Paz")));
    }

    ReceiverForModeration(NqueueForSend sender, String adminChatid, Clock clock) {
        this.sender = sender;
        this.adminChatid = adminChatid;
        this.clock = clock;
    }

    @JmsListener(destination = "queue.moderation", containerFactory = "myFactory")
    public void receiveModeration(ModerationMessage message) {
        log.info("Received moderation message: type={}, chatid={}, reason={}",
                message.getType(), message.getChatid(), message.getReason());

        if (message.getChatid() == null) {
            log.warn("Moderation message without chatid, ignoring");
            return;
        }

        String caption = buildAdminCaption(message);
        List<List<Button>> buttons = List.of(
                List.of(new Button("✅ Aprobar", "/aprobar_perfil_" + message.getChatid())),
                List.of(new Button("❌ Rechazar", "/rechazar_perfil_" + message.getChatid())),
                List.of(new Button("⛔ Bloquear usuario", "/bloquear_" + message.getChatid())));

        sender.send(adminChatid, caption, "text", message.getPhotoFileId(), null, false, buttons, null, "Markdown");
    }

    private String buildAdminCaption(ModerationMessage message) {
        StringBuilder caption = new StringBuilder();
        caption.append("🔍 *Nuevo perfil para moderar*\n\n");
        appendField(caption, "👤 Nombre", message.getName());
        appendField(caption, "🎂 Edad", computeAge(message));
        appendField(caption, "⚧ Género", translateGender(message.getGender()));
        appendField(caption, "💕 Orientación", translateOrientation(message.getOrientation()));
        appendField(caption, "📍 Ciudad", message.getCity());
        appendField(caption, "📝 Sobre", message.getDescription());
        appendField(caption, "🎸 Gustos", message.getTastes());
        appendField(caption, "🧠 Personalidad", message.getTraits());
        appendField(caption, "💘 Buscando", LookingForOption.translate(message.getLookingFor()));
        appendField(caption, "📞 Contacto", message.getContactUsername() == null ? null
                : "@" + message.getContactUsername());
        return caption.toString();
    }

    private void appendField(StringBuilder builder, String label, Object value) {
        builder.append("*").append(label).append(":* ")
                .append(value == null ? "-" : MarkdownEscaper.escape(value.toString())).append("\n");
    }

    private Integer computeAge(ModerationMessage message) {
        if (message.getBirthDate() != null) {
            try {
                LocalDate birthDate = LocalDate.parse(message.getBirthDate());
                return AgeCalculator.calculateAge(birthDate, null, clock);
            } catch (DateTimeParseException e) {
                log.warn("Invalid birthDate format in moderation message: {}", message.getBirthDate());
            }
        }
        return message.getAge();
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case "MALE" -> "Hombre";
            case "FEMALE" -> "Mujer";
            case "OTHER" -> "Otro";
            default -> gender;
        };
    }

    private String translateOrientation(String orientation) {
        return switch (orientation) {
            case "HETERO" -> "Hetero";
            case "BI" -> "Bi";
            default -> orientation;
        };
    }

}
