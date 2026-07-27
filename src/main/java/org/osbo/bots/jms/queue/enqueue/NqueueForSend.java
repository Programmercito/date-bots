package org.osbo.bots.jms.queue.enqueue;

import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageSend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

@Component
public class NqueueForSend {
    @Autowired
    private JmsTemplate jmsTemplate;

    public void send(@NonNull MessageSend message) {
        jmsTemplate.convertAndSend("queue.send", message);
    }

    public void send(@NonNull String chatid, @NonNull String text) {
        this.send(chatid, text, "text", null, null, false, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, boolean disableNotification) {
        this.send(chatid, text, "text", null, null, disableNotification, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, List<List<Button>> buttons) {
        this.send(chatid, text, "text", null, null, false, buttons);
    }

    public void send(@NonNull String chatid, @NonNull String text, boolean disableNotification,
            List<List<Button>> buttons) {
        this.send(chatid, text, "text", null, null, disableNotification, buttons);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user) {
        this.sendChannel(chatid, text, user, null, null);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user, String photo,
            String msgid) {
        List<List<Button>> buttons = null;
        if (user != null) {
            buttons = List.of(List.of(new Button("✉️ Escríbele", null, "https://t.me/" + user)));
        }
        this.send(chatid, text, "channel", photo, msgid, false, buttons);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user, String photo) {
        this.sendChannel(chatid, text, user, photo, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid) {
        this.send(chatid, text, tipo, photo, msgid, false, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid,
            boolean disableNotification) {
        this.send(chatid, text, tipo, photo, msgid, disableNotification, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid,
            boolean disableNotification, List<List<Button>> buttons) {
        this.send(chatid, text, tipo, photo, msgid, disableNotification, buttons, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid,
            boolean disableNotification, List<List<Button>> buttons, String targetProfileChatid) {
        this.send(chatid, text, tipo, photo, msgid, disableNotification, buttons, targetProfileChatid, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid,
            boolean disableNotification, List<List<Button>> buttons, String targetProfileChatid, String parseMode) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setText(text);
        message.setTipo(tipo);
        message.setMsgid(msgid);
        message.setDisableNotification(disableNotification);
        message.setButtons(buttons);
        message.setTargetProfileChatid(targetProfileChatid);
        message.setParseMode(parseMode);
        if (photo != null) {
            message.setMedias(new String[1]);
            message.getMedias()[0] = photo;
        }
        jmsTemplate.convertAndSend("queue.send", message);
    }

    public void sendMarkdown(@NonNull String chatid, @NonNull String text, List<List<Button>> buttons) {
        this.send(chatid, text, "text", null, null, false, buttons, null, "Markdown");
    }

    public void sendMarkdown(@NonNull String chatid, @NonNull String text, boolean disableNotification,
            List<List<Button>> buttons) {
        this.send(chatid, text, "text", null, null, disableNotification, buttons, null, "Markdown");
    }

    public void sendPhoto(@NonNull String chatid, @NonNull String photo, @NonNull String caption,
            boolean disableNotification, List<List<Button>> buttons, String parseMode) {
        this.send(chatid, caption, "text", photo, null, disableNotification, buttons, null, parseMode);
    }

    public void answerCallbackQuery(@NonNull String callbackQueryId) {
        MessageSend message = new MessageSend();
        message.setTipo("callback");
        message.setCallbackQueryId(callbackQueryId);
        jmsTemplate.convertAndSend("queue.send", message);
    }

    public void editMessage(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text) {
        editMessage(chatid, messageId, text, null);
    }

    public void editMessage(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons) {
        this.editMessage(chatid, messageId, text, buttons, null);
    }

    public void editMessageMarkdown(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons) {
        this.editMessage(chatid, messageId, text, buttons, "Markdown");
    }

    private void editMessage(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons, String parseMode) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setTipo("edit_text");
        message.setMessageId(messageId);
        message.setText(text);
        message.setButtons(buttons);
        message.setParseMode(parseMode);
        jmsTemplate.convertAndSend("queue.send", message);
    }

    public void editCaption(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text) {
        editCaption(chatid, messageId, text, null);
    }

    public void editCaption(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons) {
        this.editCaption(chatid, messageId, text, buttons, null);
    }

    public void editCaptionMarkdown(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons) {
        this.editCaption(chatid, messageId, text, buttons, "Markdown");
    }

    public void editPhotoCaptionMarkdown(@NonNull String chatid, @NonNull Integer messageId, @NonNull String photo,
            @NonNull String caption, List<List<Button>> buttons, String targetProfileChatid) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setTipo("edit_photo_caption");
        message.setMessageId(messageId);
        message.setText(caption);
        message.setMedias(new String[] { photo });
        message.setButtons(buttons);
        message.setTargetProfileChatid(targetProfileChatid);
        message.setParseMode("Markdown");
        jmsTemplate.convertAndSend("queue.send", message);
    }

    private void editCaption(@NonNull String chatid, @NonNull Integer messageId, @NonNull String text,
            List<List<Button>> buttons, String parseMode) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setTipo("edit_caption");
        message.setMessageId(messageId);
        message.setText(text);
        message.setButtons(buttons);
        message.setParseMode(parseMode);
        jmsTemplate.convertAndSend("queue.send", message);
    }

    public void deleteMessage(@NonNull String chatid, @NonNull Integer messageId) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setTipo("delete");
        message.setMessageId(messageId);
        jmsTemplate.convertAndSend("queue.send", message);
    }
}
