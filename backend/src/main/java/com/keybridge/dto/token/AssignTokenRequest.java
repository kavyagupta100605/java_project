package com.keybridge.dto.token;

import jakarta.validation.constraints.NotNull;

public record AssignTokenRequest(
    @NotNull(message = "User ID is required")
    Long userId
) {}
