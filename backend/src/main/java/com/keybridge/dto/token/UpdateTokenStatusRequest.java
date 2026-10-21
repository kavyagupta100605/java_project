package com.keybridge.dto.token;

import com.keybridge.enums.TokenStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTokenStatusRequest(
    @NotNull(message = "Token status is required")
    TokenStatus status
) {}
