package org.osbo.bots.jms.queue.pojos;

import lombok.Data;

@Data
public class MessageUpdate {
    private String chatid;
    private String text;
    private String user;
    private String[] medias;
}
