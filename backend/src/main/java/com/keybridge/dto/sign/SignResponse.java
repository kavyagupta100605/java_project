package com.keybridge.dto.sign;

public record SignResponse(
    Long id,
    String docHash,
    String signatureB64,
    String certFingerprint
) {}
