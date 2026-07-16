package com.slotcentral.config.controller;

import com.slotcentral.config.dto.FeatureFlagAuditLogDto;
import com.slotcentral.config.dto.FeatureFlagDto;
import com.slotcentral.config.dto.FeatureFlagUpdateRequest;
import com.slotcentral.config.service.FeatureFlagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public ResponseEntity<List<FeatureFlagDto>> getAllFlags() {
        return ResponseEntity.ok(featureFlagService.getAllFlags());
    }

    @GetMapping("/{flagName}")
    public ResponseEntity<FeatureFlagDto> getFlag(@PathVariable String flagName) {
        return ResponseEntity.ok(featureFlagService.getFlag(flagName));
    }

    @GetMapping("/{flagName}/audit")
    public ResponseEntity<List<FeatureFlagAuditLogDto>> getAuditLog(@PathVariable String flagName) {
        return ResponseEntity.ok(featureFlagService.getAuditLog(flagName));
    }

    @PutMapping("/{flagName}")
    public ResponseEntity<FeatureFlagDto> upsertFlag(
            @PathVariable String flagName,
            @Valid @RequestBody FeatureFlagUpdateRequest request) {
        return ResponseEntity.ok(featureFlagService.upsertFlag(flagName, request));
    }
}
