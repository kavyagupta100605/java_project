package com.keybridge.dto.verify;

import com.keybridge.enums.VerifyResult;

import java.time.LocalDateTime;
import java.util.List;

public record VerifyResponse(
    VerifyResult result,
    String reason,
    List<VerificationStepDto> steps,
    String signerSubject,
    String issuerSubject,
    String certificateSerialNumber,
    String documentHash,
    LocalDateTime verifiedAt
) {
    public VerifyResponse(VerifyResult result, String reason) {
        this(result, reason, List.of(), null, null, null, null, LocalDateTime.now());
    }

    public VerifyResponse(VerifyResult result, String reason, List<VerificationStepDto> steps) {
        this(result, reason, steps, null, null, null, null, LocalDateTime.now());
    }
}
