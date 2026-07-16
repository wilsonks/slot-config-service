package com.slotcentral.config.repository;

import com.slotcentral.config.domain.FeatureFlagAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureFlagAuditLogRepository extends JpaRepository<FeatureFlagAuditLog, Long> {
    List<FeatureFlagAuditLog> findByFlagNameOrderByChangedAtDesc(String flagName);
}
