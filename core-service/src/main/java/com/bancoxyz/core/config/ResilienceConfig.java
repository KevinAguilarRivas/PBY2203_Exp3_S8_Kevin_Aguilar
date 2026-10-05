package com.bancoxyz.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // 404 de cuenta inexistente no debe abrir el circuito
                .ignoreExceptions(ResponseStatusException.class)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
