package com.slotcentral.config.dto;

import java.time.LocalDateTime;

public record FeatureFlagDto(
        String name,
        String value,
        String description,
        LocalDateTime updatedAt,
        String updatedBy
) {}
