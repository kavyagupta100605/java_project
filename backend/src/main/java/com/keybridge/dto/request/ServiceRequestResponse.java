package com.keybridge.dto.request;

import com.keybridge.entity.ServiceRequest;
import com.keybridge.enums.RequestStatus;
import com.keybridge.enums.RequestType;

import java.time.LocalDateTime;

public record ServiceRequestResponse(
    Long id,
    Long userId,
    String userName,
    String userEmail,
    Long certId,
    RequestType type,
    RequestStatus status,
    String remarks,
    String adminNote,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ServiceRequestResponse fromEntity(ServiceRequest req) {
        if (req == null) return null;
        return new ServiceRequestResponse(
            req.getId(),
            req.getUser() != null ? req.getUser().getId() : null,
            req.getUser() != null ? req.getUser().getName() : null,
            req.getUser() != null ? req.getUser().getEmail() : null,
            req.getCertId(),
            req.getType(),
            req.getStatus(),
            req.getRemarks(),
            req.getAdminNote(),
            req.getCreatedAt(),
            req.getUpdatedAt()
        );
    }
}
