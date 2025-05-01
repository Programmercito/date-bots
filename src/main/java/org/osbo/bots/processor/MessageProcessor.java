package org.osbo.bots.processor;

import com.pengrad.telegrambot.model.Update;

import org.osbo.bots.jms.queue.enqueue.NqueueForProcess;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.springframework.stereotype.Component;

@Component
public class MessageProcessor {
    private NqueueForProcess processMessage;

    MessageProcessor(NqueueForProcess processMessage) {
        this.processMessage = processMessage;
    }

    public void process(Update update) {
        MessageUpdate message = new MessageUpdate();
        if (update.message() == null) {
            return;
        }
        message.setChatid(String.valueOf(update.message().chat().id()));
        if (update.message().caption() != null) {
            message.setText(update.message().caption());
        } else {
            message.setText(update.message().text());
        }
        if (update.message().photo() != null) {
            message.setMedias(new String[1]);
            message.getMedias()[0] = update.message().photo()[0].fileId();
        }
        message.setUser(update.message().from().username());
        this.processMessage.process(message);
    }
}
