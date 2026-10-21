package com.keybridge.dto.dashboard;

import java.util.List;

public record DashboardStatsDto(
    long totalCerts,
    long valid,
    long expiring,
    long expired,
    long revoked,
    long totalTokens,
    long assignedTokens,
    long pendingRequests,
    List<RecentVerificationItem> recentVerifications
) {}
