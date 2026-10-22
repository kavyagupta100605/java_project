package com.keybridge.dto.verify;

public record VerificationStepDto(
    String name,
    String status,
    String detail,
    boolean passed
) {
    public VerificationStepDto(String name, String status, String detail) {
        this(name, status, detail, "PASSED".equalsIgnoreCase(status) || "PASS".equalsIgnoreCase(status) || "VALID".equalsIgnoreCase(status));
    }
}
