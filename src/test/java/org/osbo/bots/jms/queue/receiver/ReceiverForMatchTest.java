package org.osbo.bots.jms.queue.receiver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.osbo.bots.model.services.LikeMatchService;

class ReceiverForMatchTest {

    private LikeMatchService likeMatchService;
    private ReceiverForMatch receiver;

    @BeforeEach
    void setUp() {
        likeMatchService = mock(LikeMatchService.class);
        receiver = new ReceiverForMatch(likeMatchService);
    }

    @Test
    void shouldDelegateMatchMessageToService() {
        MatchMessage message = new MatchMessage("chatid-a", "chatid-b");

        receiver.receiveMatch(message);

        verify(likeMatchService).notifyMatch(message);
    }

}
