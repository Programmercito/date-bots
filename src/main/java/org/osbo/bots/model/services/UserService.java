package org.osbo.bots.model.services;

import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public User persiste(User user) {
        User existingUser = userRepository.findById(user.getChatid()).orElse(null);
        if (existingUser != null) {
            return existingUser;
        } else {
            user.setEstado("activo");
            return userRepository.save(user);
        }
    }
}
