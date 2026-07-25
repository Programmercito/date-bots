package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to queue.like to process a like and detect mutual matches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeMessage {

    private String fromChatid;

    private String toChatid;

}
