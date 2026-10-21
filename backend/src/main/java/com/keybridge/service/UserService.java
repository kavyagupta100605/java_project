package com.keybridge.service;

import com.keybridge.dto.user.CreateUserRequest;
import com.keybridge.dto.user.UpdateUserRequest;
import com.keybridge.dto.user.UserResponse;
import com.keybridge.entity.User;
import com.keybridge.exception.BadRequestException;
import com.keybridge.exception.NotFoundException;
import com.keybridge.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        String email = req.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already in use: " + email);
        }

        User user = new User(
                req.name().trim(),
                email,
                passwordEncoder.encode(req.password()),
                req.role()
        );
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        if (req.role() != null) {
            user.setRole(req.role());
        }
        if (req.active() != null) {
            user.setActive(req.active());
        }
        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name().trim());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }

        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }
}
