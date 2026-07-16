package com.slotcentral.config.controller;

import com.slotcentral.config.config.SecurityConfig;
import com.slotcentral.config.dto.FeatureFlagDto;
import com.slotcentral.config.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeatureFlagController.class)
@Import(SecurityConfig.class)
class FeatureFlagControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FeatureFlagService featureFlagService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void getFlag_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/flags/testFlag"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upsertFlag_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/flags/testFlag")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"true\", \"updatedBy\": \"user\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void upsertFlag_withAdminRole_returns200() throws Exception {
        FeatureFlagDto mockDto = new FeatureFlagDto(
                "testFlag", "true", null, LocalDateTime.now(), "admin");
        when(featureFlagService.upsertFlag(eq("testFlag"), any()))
                .thenReturn(mockDto);

        mockMvc.perform(put("/api/v1/flags/testFlag")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"true\", \"updatedBy\": \"admin\"}"))
                .andExpect(status().isOk());
    }
}
