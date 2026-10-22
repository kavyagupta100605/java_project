package com.keybridge.dto.audit;

import com.keybridge.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    Long userId,
    String username,
    String action,
    String details,
    String ip,
    String ipAddress,
    LocalDateTime createdAt,
    LocalDateTime timestamp
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        return fromEntity(log, null);
    }

    public static AuditLogResponse fromEntity(AuditLog log, String username) {
        if (log == null) return null;
        return new AuditLogResponse(
            log.getId(),
            log.getUserId(),
            username != null ? username : (log.getUserId() != null ? "User #" + log.getUserId() : "System"),
            log.getAction(),
            log.getDetails(),
            log.getIp(),
            log.getIp(),
            log.getCreatedAt(),
            log.getCreatedAt()
        );
    }
}
