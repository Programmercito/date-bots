package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to queue.match to notify both users of a mutual match.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchMessage {

    private String chatidA;

    private String chatidB;

}
