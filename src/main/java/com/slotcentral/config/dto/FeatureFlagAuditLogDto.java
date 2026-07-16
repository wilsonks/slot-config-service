package com.slotcentral.config.dto;

import java.time.LocalDateTime;

public record FeatureFlagAuditLogDto(
        String flagName,
        String oldValue,
        String newValue,
        String changedBy,
        LocalDateTime changedAt
) {}
