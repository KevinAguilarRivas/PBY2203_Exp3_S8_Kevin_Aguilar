package com.bancoxyz.core.controller;

import com.bancoxyz.core.dto.ActualizarSaldoRequest;
import com.bancoxyz.core.dto.CuentaInteresDTO;
import com.bancoxyz.core.dto.MovimientoAnualDTO;
import com.bancoxyz.core.event.RetiroEvent;
import com.bancoxyz.core.kafka.RetiroEventProducer;
import com.bancoxyz.core.model.CuentaInteres;
import com.bancoxyz.core.model.EstadoCuentaAnual;
import com.bancoxyz.core.repository.CuentaInteresRepository;
import com.bancoxyz.core.repository.EstadoCuentaAnualRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint interno de cuentas. Incluye Circuit Breaker de Resilience4j
 * en la consulta de interes, y publica evento Kafka al actualizar saldo.
 */
@RestController
@RequestMapping("/internal/cuentas")
public class CuentasController {

    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final RetiroEventProducer retiroEventProducer;

    public CuentasController(CuentaInteresRepository cuentaRepo,
                             EstadoCuentaAnualRepository estadoRepo,
                             RetiroEventProducer retiroEventProducer) {
        this.cuentaInteresRepository = cuentaRepo;
        this.estadoCuentaAnualRepository = estadoRepo;
        this.retiroEventProducer = retiroEventProducer;
    }

    /**
     * Circuit Breaker activo: si la BD no responde en varios intentos,
     * el circuito se abre y se retorna fallback en lugar de bloquear.
     */
    @CircuitBreaker(name = "cuentas", fallbackMethod = "fallbackInteres")
    @GetMapping("/{cuentaId}/interes")
    public CuentaInteresDTO obtenerInteres(@PathVariable Long cuentaId) {
        return toDTO(buscarCuentaOFallar(cuentaId));
    }

    public CuentaInteresDTO fallbackInteres(Long cuentaId, Throwable t) {
        // Cuenta inexistente (404): no es una falla del servicio, se propaga tal cual
        if (t instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            throw rse;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio de cuentas temporalmente no disponible. Intente nuevamente.");
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoAnualDTO> obtenerMovimientos(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) Integer anio) {
        List<EstadoCuentaAnual> movimientos;
        if (anio != null) {
            movimientos = estadoCuentaAnualRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                    cuentaId, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31));
        } else {
            movimientos = estadoCuentaAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId);
        }
        return movimientos.stream()
                .map(m -> new MovimientoAnualDTO(m.getCuentaId(), m.getFecha(),
                        m.getTipoOperacion(), m.getMonto(), m.getDescripcion(), m.isAnomalia()))
                .toList();
    }

    /**
     * Actualiza el saldo y publica el evento de retiro en Kafka.
     * Solo el BFF Cajero invoca este endpoint tras validar el retiro.
     */
    @PatchMapping("/{cuentaId}/saldo")
    public CuentaInteresDTO actualizarSaldo(@PathVariable Long cuentaId,
                                            @RequestBody ActualizarSaldoRequest request) {
        CuentaInteres cuenta = buscarCuentaOFallar(cuentaId);
        var saldoAnterior = cuenta.getSaldoFinal();
        cuenta.setSaldoFinal(request.nuevoSaldo());
        cuentaInteresRepository.save(cuenta);

        // Publicar evento de retiro en Kafka
        retiroEventProducer.publicarRetiro(new RetiroEvent(
                cuentaId,
                saldoAnterior.subtract(request.nuevoSaldo()),
                saldoAnterior,
                request.nuevoSaldo(),
                Instant.now()
        ));

        return toDTO(cuenta);
    }

    private CuentaInteres buscarCuentaOFallar(Long cuentaId) {
        return cuentaInteresRepository.findFirstByCuentaIdOrderByIdDesc(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe cuenta con cuenta_id=" + cuentaId));
    }

    private CuentaInteresDTO toDTO(CuentaInteres c) {
        return new CuentaInteresDTO(c.getCuentaId(), c.getNombre(), c.getSaldoInicial(),
                c.getSaldoFinal(), c.getEdad(), c.getTipoCuenta(), c.isAnomalia());
    }
}
