package com.keybridge.dto.verify;

import com.keybridge.enums.VerifyResult;

import java.util.List;

public record VerifyResponse(
    VerifyResult result,
    String reason,
    List<VerificationStepDto> steps
) {
    public VerifyResponse(VerifyResult result, String reason) {
        this(result, reason, List.of());
    }
}
