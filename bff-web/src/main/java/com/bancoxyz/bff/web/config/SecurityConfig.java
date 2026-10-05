package com.bancoxyz.bff.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion OAuth2 Resource Server para el BFF Web.
 * Todos los endpoints /web/** requieren un JWT valido emitido por Keycloak
 * (realm bancoxyz). El token se obtiene previamente con el flujo
 * Authorization Code o Client Credentials de OAuth2.
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
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {}) // valida JWT contra Keycloak (issuer-uri en config-server)
            );
        return http.build();
    }
}
