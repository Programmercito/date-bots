package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to queue.analytics to update daily usage counters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMessage {

    private String chatid;

    private String date;

    private String eventType;

    private int increment;

}
