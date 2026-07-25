package org.osbo.bots.jms.queue.receiver;

import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
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

    ReceiverForModeration(NqueueForSend sender, @Value("${telegram.admin}") String adminChatid) {
        this.sender = sender;
        this.adminChatid = adminChatid;
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

        sender.send(adminChatid, caption, "text", message.getPhotoFileId(), null, false, buttons);
    }

    private String buildAdminCaption(ModerationMessage message) {
        StringBuilder caption = new StringBuilder();
        caption.append("Nuevo perfil para moderar:\n\n");
        appendField(caption, "Nombre", message.getName());
        appendField(caption, "Edad", message.getAge());
        appendField(caption, "Género", translateGender(message.getGender()));
        appendField(caption, "Orientación", translateOrientation(message.getOrientation()));
        appendField(caption, "Ciudad", message.getCity());
        appendField(caption, "Sobre", message.getDescription());
        appendField(caption, "Gustos", message.getTastes());
        appendField(caption, "Personalidad", message.getTraits());
        appendField(caption, "Buscando", message.getLookingFor());
        appendField(caption, "Contacto", message.getContactUsername() == null ? null
                : "@" + message.getContactUsername());
        return caption.toString();
    }

    private void appendField(StringBuilder builder, String label, Object value) {
        builder.append(label).append(": ").append(value == null ? "-" : value).append("\n");
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
