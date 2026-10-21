package com.keybridge.dto.audit;

import com.keybridge.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    Long userId,
    String action,
    String details,
    String ip,
    LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) return null;
        return new AuditLogResponse(
            log.getId(),
            log.getUserId(),
            log.getAction(),
            log.getDetails(),
            log.getIp(),
            log.getCreatedAt()
        );
    }
}
