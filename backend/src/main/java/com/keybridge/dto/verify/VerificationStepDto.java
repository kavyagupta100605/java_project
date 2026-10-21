package com.keybridge.dto.verify;

public record VerificationStepDto(
    String name,
    String status,
    String detail
) {}
