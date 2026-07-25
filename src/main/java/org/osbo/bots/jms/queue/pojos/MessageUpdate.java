package org.osbo.bots.jms.queue.pojos;

import lombok.Data;

@Data
public class MessageUpdate {
    private String chatid;
    private String text;
    private String user;
    private String[] medias;
    private String callbackQueryId;

    /**
     * Telegram message ID when the update comes from a callback query.
     * Used to edit/delete discovery profile messages.
     */
    private Integer messageId;
}
