package com.keybridge.dto.dashboard;

import com.keybridge.enums.VerifyResult;

import java.time.LocalDateTime;

public record RecentVerificationItem(
    String file,
    VerifyResult result,
    LocalDateTime at
) {}
