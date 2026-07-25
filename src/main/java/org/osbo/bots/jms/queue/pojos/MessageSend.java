package org.osbo.bots.jms.queue.pojos;

import java.util.List;

import lombok.Data;

@Data
public class MessageSend {
    public String chatid;
    public String text;
    public String[] medias;
    public String tipo;
    public String msgid;
    public boolean disableNotification;
    public String callbackQueryId;
    public List<List<Button>> buttons;

    /**
     * Telegram message ID for editing/deleting existing messages.
     */
    public Integer messageId;

    /**
     * Target profile chat ID for discovery_profile messages.
     * Used to deactivate the profile when its photo file_id is broken.
     */
    public String targetProfileChatid;
}
