package com.slotcentral.config.service;

import com.slotcentral.config.domain.FeatureFlag;
import com.slotcentral.config.domain.FeatureFlagAuditLog;
import com.slotcentral.config.dto.FeatureFlagAuditLogDto;
import com.slotcentral.config.dto.FeatureFlagDto;
import com.slotcentral.config.dto.FeatureFlagUpdateRequest;
import com.slotcentral.config.repository.FeatureFlagAuditLogRepository;
import com.slotcentral.config.repository.FeatureFlagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagAuditLogRepository auditLogRepository;

    public FeatureFlagService(FeatureFlagRepository flagRepository,
                               FeatureFlagAuditLogRepository auditLogRepository) {
        this.flagRepository = flagRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public FeatureFlagDto getFlag(String name) {
        return flagRepository.findByName(name)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Feature flag not found: " + name));
    }

    @Transactional
    public FeatureFlagDto upsertFlag(String name, FeatureFlagUpdateRequest request) {
        FeatureFlag flag = flagRepository.findByName(name).orElse(null);
        String oldValue = (flag != null) ? flag.getValue() : null;

        if (flag == null) {
            flag = new FeatureFlag();
            flag.setName(name);
        }

        flag.setValue(request.value());
        flag.setUpdatedBy(request.updatedBy());
        flag = flagRepository.save(flag);

        FeatureFlagAuditLog auditLog = new FeatureFlagAuditLog();
        auditLog.setFlagName(name);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(request.value());
        auditLog.setChangedBy(request.updatedBy());
        auditLogRepository.save(auditLog);

        return toDto(flag);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getAllFlags() {
        return flagRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagAuditLogDto> getAuditLog(String name) {
        flagRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Feature flag not found: " + name));
        return auditLogRepository.findByFlagNameOrderByChangedAtDesc(name).stream()
                .map(this::toAuditDto)
                .toList();
    }

    private FeatureFlagDto toDto(FeatureFlag flag) {
        return new FeatureFlagDto(
                flag.getName(),
                flag.getValue(),
                flag.getDescription(),
                flag.getUpdatedAt(),
                flag.getUpdatedBy()
        );
    }

    private FeatureFlagAuditLogDto toAuditDto(FeatureFlagAuditLog log) {
        return new FeatureFlagAuditLogDto(
                log.getFlagName(),
                log.getOldValue(),
                log.getNewValue(),
                log.getChangedBy(),
                log.getChangedAt()
        );
    }
}
