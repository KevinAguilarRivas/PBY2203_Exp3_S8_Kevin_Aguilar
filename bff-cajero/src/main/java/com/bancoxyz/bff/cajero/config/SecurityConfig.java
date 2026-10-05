package com.bancoxyz.bff.cajero.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server para BFF Cajero.
 * Todos los endpoints /cajero/** requieren JWT emitido por Keycloak.
 * En semanas anteriores se usaba credencial de dispositivo (header X-Cajero-Id);
 * aqui se reemplaza por OAuth2 segun requerimiento de la Semana 8.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
