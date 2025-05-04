package org.osbo.bots.model.services;

import java.util.List;

import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.repositories.MessageRepository;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message save(Message message) {
        return messageRepository.save(message);
    }

    public Message findById(String id) {
        return messageRepository.findById(id).orElse(null);
    }

    public int existsMessageInLastHour(String user) {
        return messageRepository.existsMessageInLastHour(user);
    }

    public List<Message> vencidos() {
        return messageRepository.vencidos();
    }
}