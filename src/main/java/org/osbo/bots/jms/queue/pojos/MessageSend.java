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
}
