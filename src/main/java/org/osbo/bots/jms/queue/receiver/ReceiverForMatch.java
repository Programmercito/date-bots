package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.osbo.bots.model.services.LikeMatchService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code queue.match} messages and delegates match notification to the
 * {@link LikeMatchService}.
 */
@Component
public class ReceiverForMatch {

    private final LikeMatchService likeMatchService;

    public ReceiverForMatch(LikeMatchService likeMatchService) {
        this.likeMatchService = likeMatchService;
    }

    @JmsListener(destination = "queue.match", containerFactory = "myFactory")
    public void receiveMatch(MatchMessage message) {
        likeMatchService.notifyMatch(message);
    }

}
