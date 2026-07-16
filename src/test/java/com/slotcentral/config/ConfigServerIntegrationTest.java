package com.slotcentral.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "native"})
class ConfigServerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void gameEngineServiceConfig_returnsOkAndContainsGaffingProperty() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/game-engine-service/default", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("game.engine.gaffing.enabled");
    }

    @Test
    void jackpotServiceConfig_returnsOkAndContainsTiers() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/jackpot-service/default", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jackpot.tiers");
    }
}
