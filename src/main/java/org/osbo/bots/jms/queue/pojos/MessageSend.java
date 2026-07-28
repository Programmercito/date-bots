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

    /**
     * Optional parse mode for the message text/caption. Supported values:
     * "Markdown", "MarkdownV2", "HTML".
     */
    public String parseMode;

    /**
     * When true, ReceiverForSend will store the resulting message ID in the
     * user's {@code photoEditPromptMessageId} field so the prompt can be edited
     * in place during the photo edit flow.
     */
    public boolean saveAsPhotoEditPrompt;
}
