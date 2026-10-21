package com.keybridge.dto.dashboard;

import java.util.List;

public record SearchResponse(
    List<SearchResultItem> certificates,
    List<SearchResultItem> tokens,
    List<SearchResultItem> users
) {}
