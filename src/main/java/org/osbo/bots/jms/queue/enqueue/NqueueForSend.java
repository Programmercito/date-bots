package org.osbo.bots.jms.queue.enqueue;

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
        this.send(chatid, text, "text", null, null);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user) {
        this.send(chatid, text + "\nPuedes escribirle a :@" + user, "channel", null, null);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user, String photo,
            String msgid) {
        String add = "";
        if (user != null) {
            add = "\nPuedes escribirle a :@" + user;
        }
        this.send(chatid, text + add, "channel", photo, msgid);
    }

    public void sendChannel(@NonNull String chatid, @NonNull String text, @NonNull String user, String photo) {
        this.sendChannel(chatid, text, user, photo, null);
    }

    public void send(@NonNull String chatid, @NonNull String text, String tipo, String photo, String msgid) {
        MessageSend message = new MessageSend();
        message.setChatid(chatid);
        message.setText(text);
        message.setTipo(tipo);
        message.setMsgid(msgid);
        if (photo != null) {
            message.setMedias(new String[1]);
            message.getMedias()[0] = photo;
        }
        jmsTemplate.convertAndSend("queue.send", message);
    }
}
