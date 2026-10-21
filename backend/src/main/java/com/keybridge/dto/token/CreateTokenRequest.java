package com.keybridge.dto.token;

import jakarta.validation.constraints.NotBlank;

public record CreateTokenRequest(
    @NotBlank(message = "Serial number is required")
    String serialNo,

    String label
) {}
