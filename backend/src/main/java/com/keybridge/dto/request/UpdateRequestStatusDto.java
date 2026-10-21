package com.keybridge.dto.request;

import com.keybridge.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRequestStatusDto(
    @NotNull(message = "Target status is required")
    RequestStatus status,

    String adminNote
) {}
