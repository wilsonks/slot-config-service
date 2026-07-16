package com.slotcentral.config.service;

import com.slotcentral.config.domain.FeatureFlag;
import com.slotcentral.config.domain.FeatureFlagAuditLog;
import com.slotcentral.config.dto.FeatureFlagAuditLogDto;
import com.slotcentral.config.dto.FeatureFlagDto;
import com.slotcentral.config.dto.FeatureFlagUpdateRequest;
import com.slotcentral.config.repository.FeatureFlagAuditLogRepository;
import com.slotcentral.config.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    FeatureFlagRepository flagRepository;

    @Mock
    FeatureFlagAuditLogRepository auditLogRepository;

    @InjectMocks
    FeatureFlagService featureFlagService;

    @Test
    void getFlag_whenFound_returnsDto() {
        FeatureFlag flag = buildFlag("myFlag", "true", "test desc");
        when(flagRepository.findByName("myFlag")).thenReturn(Optional.of(flag));

        FeatureFlagDto dto = featureFlagService.getFlag("myFlag");

        assertThat(dto.name()).isEqualTo("myFlag");
        assertThat(dto.value()).isEqualTo("true");
    }

    @Test
    void getFlag_whenNotFound_throwsNotFound() {
        when(flagRepository.findByName("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featureFlagService.getFlag("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void upsertFlag_newFlag_createsAndRecordsAuditWithNullOldValue() {
        when(flagRepository.findByName("newFlag")).thenReturn(Optional.empty());

        FeatureFlag saved = buildFlag("newFlag", "false", null);
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(saved);

        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest("false", "admin");
        featureFlagService.upsertFlag("newFlag", request);

        ArgumentCaptor<FeatureFlagAuditLog> auditCaptor = ArgumentCaptor.forClass(FeatureFlagAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        FeatureFlagAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getFlagName()).isEqualTo("newFlag");
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue()).isEqualTo("false");
        assertThat(auditLog.getChangedBy()).isEqualTo("admin");
    }

    @Test
    void upsertFlag_existingFlag_updatesAndRecordsCorrectOldValue() {
        FeatureFlag existing = buildFlag("existingFlag", "oldValue", null);
        when(flagRepository.findByName("existingFlag")).thenReturn(Optional.of(existing));

        FeatureFlag updated = buildFlag("existingFlag", "newValue", null);
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(updated);

        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest("newValue", "staff");
        featureFlagService.upsertFlag("existingFlag", request);

        ArgumentCaptor<FeatureFlagAuditLog> auditCaptor = ArgumentCaptor.forClass(FeatureFlagAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        FeatureFlagAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getOldValue()).isEqualTo("oldValue");
        assertThat(auditLog.getNewValue()).isEqualTo("newValue");
    }

    private FeatureFlag buildFlag(String name, String value, String description) {
        FeatureFlag flag = new FeatureFlag();
        flag.setName(name);
        flag.setValue(value);
        flag.setDescription(description);
        flag.setUpdatedAt(LocalDateTime.now());
        flag.setUpdatedBy("system");
        return flag;
    }
}
