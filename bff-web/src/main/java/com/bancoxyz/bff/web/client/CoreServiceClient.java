package com.bancoxyz.bff.web.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Cliente HTTP hacia core-service con Circuit Breaker.
 * Si core-service no responde, el circuito se abre y se retorna
 * una respuesta de fallback en lugar de propagar el error.
 */
@Service
public class CoreServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CoreServiceClient.class);
    private final RestClient restClient;

    public CoreServiceClient(RestClient coreRestClient) {
        this.restClient = coreRestClient;
    }

    @CircuitBreaker(name = "core", fallbackMethod = "fallbackCuenta")
    public CoreCuentaInteresDTO obtenerInteres(Long cuentaId) {
        return restClient.get()
                .uri("/internal/cuentas/{id}/interes", cuentaId)
                .retrieve()
                .body(CoreCuentaInteresDTO.class);
    }

    public CoreCuentaInteresDTO fallbackCuenta(Long cuentaId, Throwable t) {
        propagarErrorCliente(t);
        log.warn("[CircuitBreaker] core-service no disponible para cuentaId={}: {}", cuentaId, t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de cuentas no está disponible temporalmente. Intente más tarde.");
    }

    @CircuitBreaker(name = "core", fallbackMethod = "fallbackMovimientos")
    public List<CoreMovimientoAnualDTO> obtenerMovimientos(Long cuentaId) {
        return restClient.get()
                .uri("/internal/cuentas/{id}/movimientos", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<CoreMovimientoAnualDTO> fallbackMovimientos(Long cuentaId, Throwable t) {
        propagarErrorCliente(t);
        log.warn("[CircuitBreaker] movimientos no disponibles para cuentaId={}", cuentaId);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El historial de movimientos no está disponible temporalmente.");
    }

    @CircuitBreaker(name = "core", fallbackMethod = "fallbackTransacciones")
    public List<CoreTransaccionDiariaDTO> obtenerTransacciones() {
        return restClient.get()
                .uri("/internal/transacciones-diarias")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<CoreTransaccionDiariaDTO> fallbackTransacciones(Throwable t) {
        propagarErrorCliente(t);
        log.warn("[CircuitBreaker] transacciones no disponibles");
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de transacciones no está disponible temporalmente.");
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
