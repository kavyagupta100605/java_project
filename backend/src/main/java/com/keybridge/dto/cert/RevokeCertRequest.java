package com.keybridge.dto.cert;

import jakarta.validation.constraints.NotBlank;

public record RevokeCertRequest(
    @NotBlank(message = "Revocation reason is required")
    String reason
) {}
