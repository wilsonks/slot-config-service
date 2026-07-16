package com.slotcentral.config.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.jwt.jwks-uri}")
    private String jwksUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is not required: this service uses stateless JWT ******
            // so no session cookie can be hijacked via cross-site request forgery.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health/info open for load-balancer probes
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // API endpoints (specific rules must precede broad Config Server patterns)
                .requestMatchers(HttpMethod.GET, "/api/v1/flags/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/flags/**").hasAnyRole("ADMIN", "STAFF")
                // Config Server endpoints: protected by network boundary, not user auth
                .requestMatchers(HttpMethod.GET,
                    "/{application}/{profile}",
                    "/{application}/{profile}/**",
                    "/{application}-{profile}.yml",
                    "/{label}/{application}-{profile}.yml").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}
