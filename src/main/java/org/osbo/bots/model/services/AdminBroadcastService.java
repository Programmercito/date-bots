package org.osbo.bots.model.services;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminBroadcastService {

    private static final String COMMAND_BROADCAST_ALL = "/enviar_todos";
    private static final String COMMAND_BROADCAST_CHANNEL = "/enviar_canal";

    private final NqueueForSend sender;
    private final UserService userService;
    private final String adminChatid;
    private final String channel;

    public AdminBroadcastService(NqueueForSend sender, UserService userService,
            @Value("${telegram.admin}") String adminChatid,
            @Value("${telegram.channel}") String channel) {
        this.sender = sender;
        this.userService = userService;
        this.adminChatid = adminChatid;
        this.channel = channel;
    }

    public boolean handle(String chatid, String text) {
        if (text == null || !chatid.equals(adminChatid)) {
            return false;
        }

        if (text.startsWith(COMMAND_BROADCAST_ALL + " ")) {
            String message = text.substring(COMMAND_BROADCAST_ALL.length()).trim();
            if (!message.isBlank()) {
                broadcastToAll(message);
            }
            return true;
        }

        if (text.startsWith(COMMAND_BROADCAST_CHANNEL + " ")) {
            String message = text.substring(COMMAND_BROADCAST_CHANNEL.length()).trim();
            if (!message.isBlank()) {
                sendToChannel(message);
            }
            return true;
        }

        return false;
    }

    private void broadcastToAll(String message) {
        for (User user : userService.findAll()) {
            sender.sendMarkdown(user.getChatid(), message, false, null);
        }
    }

    private void sendToChannel(String message) {
        sender.send(channel, message, "text", null, null, false, null, null, "Markdown");
    }
}
