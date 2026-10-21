package com.keybridge.dto.verify;

import com.keybridge.entity.VerificationLog;
import com.keybridge.enums.VerifyResult;

import java.time.LocalDateTime;

public record VerificationLogResponse(
    Long id,
    Long verifiedById,
    String verifiedByName,
    String fileName,
    Long certId,
    VerifyResult result,
    String reason,
    LocalDateTime verifiedAt
) {
    public static VerificationLogResponse fromEntity(VerificationLog log) {
        if (log == null) return null;
        return new VerificationLogResponse(
            log.getId(),
            log.getVerifiedBy() != null ? log.getVerifiedBy().getId() : null,
            log.getVerifiedBy() != null ? log.getVerifiedBy().getName() : null,
            log.getFileName(),
            log.getCertId(),
            log.getResult(),
            log.getReason(),
            log.getVerifiedAt()
        );
    }
}
