package com.devops.userservice.service;

import com.devops.userservice.model.User;
import com.devops.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        log.info("Registering new user with email: {}", user.getEmail());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", user.getEmail());
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }

        User saved = userRepository.save(user);
        log.info("User registered successfully — id: {}, email: {}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Map<String, Object> login(String email, String password) {
        log.info("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found: {}", email);
                    return new RuntimeException("User not found: " + email);
                });

        if (!user.getPassword().equals(password)) {
            log.warn("Login failed — invalid password for: {}", email);
            throw new RuntimeException("Invalid password");
        }

        log.info("Login successful for userId: {}", user.getId());
        return Map.of(
                "message", "Login successful",
                "userId", user.getId(),
                "name", user.getName()
        );
    }

    public User findById(Long id) {
        log.info("Fetching user by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found — id: {}", id);
                    return new RuntimeException("User not found: " + id);
                });
    }

    public List<User> findAll() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
    }
}
