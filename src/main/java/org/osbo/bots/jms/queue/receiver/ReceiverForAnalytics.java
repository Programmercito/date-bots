package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.AnalyticsMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Skeleton consumer for queue.analytics.
 * Real daily limit tracking will be implemented in a later phase.
 */
@Slf4j
@Component
public class ReceiverForAnalytics {

    @JmsListener(destination = "queue.analytics", containerFactory = "myFactory")
    public void receiveAnalytics(AnalyticsMessage message) {
        log.info("Received analytics message: chatid={}, date={}, eventType={}, increment={}",
                message.getChatid(), message.getDate(), message.getEventType(), message.getIncrement());
    }

}
