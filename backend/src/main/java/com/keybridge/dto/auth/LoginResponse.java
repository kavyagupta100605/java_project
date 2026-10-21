package com.keybridge.dto.auth;

import com.keybridge.dto.user.UserResponse;

public record LoginResponse(
    String token,
    UserResponse user
) {}
