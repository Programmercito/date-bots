package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Skeleton consumer for queue.moderation.
 * Real admin moderation will be implemented in a later phase.
 */
@Slf4j
@Component
public class ReceiverForModeration {

    @JmsListener(destination = "queue.moderation", containerFactory = "myFactory")
    public void receiveModeration(ModerationMessage message) {
        log.info("Received moderation message: type={}, chatid={}, reason={}",
                message.getType(), message.getChatid(), message.getReason());
    }

}
