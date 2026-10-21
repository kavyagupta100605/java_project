package com.keybridge.dto.cert;

import jakarta.validation.constraints.NotBlank;

public record IssueCertRequest(
    Long userId,

    @NotBlank(message = "Common Name is required")
    String commonName,

    Integer validDays,
    Integer validityDays,
    String organization,
    String country,
    Long tokenId
) {
    public IssueCertRequest(Long userId, String commonName, int validDays, Long tokenId) {
        this(userId, commonName, validDays, validDays, null, null, tokenId);
    }

    public int resolvedDays() {
        if (validityDays != null && validityDays > 0) return validityDays;
        if (validDays != null && validDays > 0) return validDays;
        return 365;
    }
}
