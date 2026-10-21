package com.keybridge.service;

import com.keybridge.dto.auth.LoginRequest;
import com.keybridge.dto.auth.LoginResponse;
import com.keybridge.dto.user.UserResponse;
import com.keybridge.entity.User;
import com.keybridge.exception.ForbiddenException;
import com.keybridge.exception.NotFoundException;
import com.keybridge.repository.UserRepository;
import com.keybridge.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generate(user);
        return new LoginResponse(token, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ForbiddenException("Not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserResponse.fromEntity(getCurrentUserEntity());
    }
}
