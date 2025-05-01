package org.osbo.bots.jms.queue.enqueue;

import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

@Component
public class NqueueForProcess {
    @Autowired
    private JmsTemplate jmsTemplate;

    public void process(@NonNull MessageUpdate message) {
        jmsTemplate.convertAndSend("queue.process", message);
    }
}
