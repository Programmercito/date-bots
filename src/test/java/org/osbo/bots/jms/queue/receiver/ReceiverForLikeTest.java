package org.osbo.bots.jms.queue.receiver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.model.services.LikeMatchService;

class ReceiverForLikeTest {

    private LikeMatchService likeMatchService;
    private ReceiverForLike receiver;

    @BeforeEach
    void setUp() {
        likeMatchService = mock(LikeMatchService.class);
        receiver = new ReceiverForLike(likeMatchService);
    }

    @Test
    void shouldDelegateLikeMessageToService() {
        LikeMessage message = new LikeMessage("from-123", "to-456");

        receiver.receiveLike(message);

        verify(likeMatchService).processLike(message);
    }

}
