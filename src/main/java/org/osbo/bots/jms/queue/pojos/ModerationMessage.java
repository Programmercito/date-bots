package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to queue.moderation for admin review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationMessage {

    private String type;

    private String chatid;

    private String reason;

}
