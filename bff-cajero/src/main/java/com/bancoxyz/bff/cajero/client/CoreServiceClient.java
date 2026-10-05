package com.bancoxyz.bff.cajero.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cliente hacia core-service con Circuit Breaker + Retry.
 * El canal Cajero es critico: se reintenta hasta 3 veces antes de abrir
 * el circuito, para tolerar microfallos de red transitorios.
 */
@Service
public class CoreServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CoreServiceClient.class);
    private final RestClient restClient;

    public CoreServiceClient(RestClient coreRestClient) {
        this.restClient = coreRestClient;
    }

    @CircuitBreaker(name = "core", fallbackMethod = "fallbackCuenta")
    @Retry(name = "core")
    public CoreCuentaInteresDTO obtenerInteres(Long cuentaId) {
        return restClient.get()
                .uri("/internal/cuentas/{id}/interes", cuentaId)
                .retrieve().body(CoreCuentaInteresDTO.class);
    }

    public CoreCuentaInteresDTO fallbackCuenta(Long cuentaId, Throwable t) {
        propagarErrorCliente(t);
        log.warn("[CB] core no disponible para cajero cuentaId={}: {}", cuentaId, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Cajero temporalmente fuera de servicio. Intente en otro ATM.");
    }

    @CircuitBreaker(name = "core", fallbackMethod = "fallbackSaldo")
    @Retry(name = "core")
    public CoreCuentaInteresDTO actualizarSaldo(Long cuentaId, BigDecimal nuevoSaldo) {
        return restClient.patch()
                .uri("/internal/cuentas/{id}/saldo", cuentaId)
                .body(Map.of("nuevoSaldo", nuevoSaldo))
                .retrieve().body(CoreCuentaInteresDTO.class);
    }

    public CoreCuentaInteresDTO fallbackSaldo(Long cuentaId, BigDecimal nuevoSaldo, Throwable t) {
        propagarErrorCliente(t);
        log.error("[CB] No se pudo actualizar saldo cuentaId={}: {}", cuentaId, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo procesar el retiro. Intente nuevamente.");
    }

    /**
     * Un 4xx de core-service (ej. 404 cuenta inexistente) es un error del cliente,
     * no una caida del servicio: se propaga tal cual en vez de responder 503.
     */
    private static void propagarErrorCliente(Throwable t) {
        if (t instanceof HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getStatusText());
        }
    }
}
