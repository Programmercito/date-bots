package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.model.services.LikeMatchService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code queue.like} messages and delegates match processing to the
 * {@link LikeMatchService}.
 */
@Component
public class ReceiverForLike {

    private final LikeMatchService likeMatchService;

    public ReceiverForLike(LikeMatchService likeMatchService) {
        this.likeMatchService = likeMatchService;
    }

    @JmsListener(destination = "queue.like", containerFactory = "myFactory")
    public void receiveLike(LikeMessage message) {
        likeMatchService.processLike(message);
    }

}
