package com.keybridge.dto.user;

import com.keybridge.enums.Role;

public record UpdateUserRequest(
    Role role,
    Boolean active,
    String name,
    String password
) {}
