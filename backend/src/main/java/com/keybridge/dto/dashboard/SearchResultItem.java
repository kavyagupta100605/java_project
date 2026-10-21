package com.keybridge.dto.dashboard;

public record SearchResultItem(
    Long id,
    String title,
    String subtitle,
    String type,
    String url
) {}
