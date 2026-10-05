package com.bancoxyz.bff.mobile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${core-service.url:http://core-service:8080}")
    private String coreServiceUrl;

    @Bean
    public RestClient coreRestClient() {
        // Timeouts cortos: si core-service no responde, se falla rapido y
        // el Circuit Breaker contabiliza el error en vez de bloquear el hilo.
        // (JdkClientHttpRequestFactory soporta PATCH, usado en la actualizacion de saldo)
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .baseUrl(coreServiceUrl)
                .requestFactory(factory)
                .build();
    }
}
