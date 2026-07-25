package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Skeleton consumer for queue.match.
 * Real match notification will be implemented in a later phase.
 */
@Slf4j
@Component
public class ReceiverForMatch {

    @JmsListener(destination = "queue.match", containerFactory = "myFactory")
    public void receiveMatch(MatchMessage message) {
        log.info("Received match message: chatidA={}, chatidB={}", message.getChatidA(), message.getChatidB());
    }

}
