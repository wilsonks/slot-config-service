package com.slotcentral.config.dto;

import jakarta.validation.constraints.NotBlank;

public record FeatureFlagUpdateRequest(
        @NotBlank String value,
        @NotBlank String updatedBy
) {}
