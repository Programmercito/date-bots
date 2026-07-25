package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Skeleton consumer for queue.like.
 * Real match detection will be implemented in a later phase.
 */
@Slf4j
@Component
public class ReceiverForLike {

    @JmsListener(destination = "queue.like", containerFactory = "myFactory")
    public void receiveLike(LikeMessage message) {
        log.info("Received like message: fromChatid={}, toChatid={}", message.getFromChatid(), message.getToChatid());
    }

}
