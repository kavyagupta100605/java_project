package com.keybridge.dto.user;

import com.keybridge.entity.User;
import com.keybridge.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String name,
    String email,
    Role role,
    boolean active,
    LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()
        );
    }
}
