package com.keybridge.dto.request;

import com.keybridge.enums.RequestType;
import jakarta.validation.constraints.NotNull;

public record CreateRequestDto(
    @NotNull(message = "Request type is required")
    RequestType type,

    Long certId,
    String remarks
) {}
