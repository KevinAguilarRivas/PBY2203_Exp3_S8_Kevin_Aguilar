package com.bancoxyz.core.service;

import com.bancoxyz.core.dto.ActualizarSaldoRequest;
import com.bancoxyz.core.dto.CuentaInteresDTO;
import com.bancoxyz.core.dto.MovimientoAnualDTO;
import com.bancoxyz.core.dto.TransaccionDiariaDTO;
import com.bancoxyz.core.event.RetiroEvent;
import com.bancoxyz.core.kafka.RetiroEventProducer;
import com.bancoxyz.core.model.CuentaInteres;
import com.bancoxyz.core.model.EstadoCuentaAnual;
import com.bancoxyz.core.model.ResumenTransaccion;
import com.bancoxyz.core.repository.CuentaInteresRepository;
import com.bancoxyz.core.repository.EstadoCuentaAnualRepository;
import com.bancoxyz.core.repository.ResumenTransaccionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoreService {

    private static final Logger log = LoggerFactory.getLogger(CoreService.class);

    private final CuentaInteresRepository cuentaRepository;
    private final EstadoCuentaAnualRepository estadoRepository;
    private final ResumenTransaccionRepository transaccionRepository;
    private final RetiroEventProducer retiroEventProducer;

    public CoreService(CuentaInteresRepository cuentaRepository,
                       EstadoCuentaAnualRepository estadoRepository,
                       ResumenTransaccionRepository transaccionRepository,
                       RetiroEventProducer retiroEventProducer) {
        this.cuentaRepository = cuentaRepository;
        this.estadoRepository = estadoRepository;
        this.transaccionRepository = transaccionRepository;
        this.retiroEventProducer = retiroEventProducer;
    }

    // -----------------------------------------------------------------------
    // Consulta de cuenta con intereses
    // -----------------------------------------------------------------------

    @CircuitBreaker(name = "coreService", fallbackMethod = "obtenerInteresFallback")
    public CuentaInteresDTO obtenerInteresCuenta(Long id) {
        CuentaInteres cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + id));
        return mapToCuentaDTO(cuenta);
    }

    public CuentaInteresDTO obtenerInteresFallback(Long id, Throwable ex) {
        log.warn("CB activo para obtenerInteresCuenta({}): {}", id, ex.getMessage());
        return new CuentaInteresDTO(id, "Servicio no disponible",
                BigDecimal.ZERO, BigDecimal.ZERO, 0, "N/D", false);
    }

    // -----------------------------------------------------------------------
    // Consulta de movimientos anuales
    // -----------------------------------------------------------------------

    @CircuitBreaker(name = "coreService", fallbackMethod = "obtenerMovimientosFallback")
    public List<MovimientoAnualDTO> obtenerMovimientos(Long cuentaId) {
        List<EstadoCuentaAnual> movimientos = estadoRepository.findByCuentaIdOrderByFechaDesc(cuentaId);
        return movimientos.stream().map(this::mapToMovimientoDTO).collect(Collectors.toList());
    }

    public List<MovimientoAnualDTO> obtenerMovimientosFallback(Long cuentaId, Throwable ex) {
        log.warn("CB activo para obtenerMovimientos({}): {}", cuentaId, ex.getMessage());
        return Collections.emptyList();
    }

    // -----------------------------------------------------------------------
    // Consulta de transacciones diarias
    // -----------------------------------------------------------------------

    @CircuitBreaker(name = "coreService", fallbackMethod = "obtenerTransaccionesFallback")
    public List<TransaccionDiariaDTO> obtenerTransaccionesDiarias() {
        LocalDate hoy = LocalDate.now();
        List<ResumenTransaccion> transacciones = transaccionRepository.findByFecha(hoy);
        return transacciones.stream().map(this::mapToTransaccionDTO).collect(Collectors.toList());
    }

    public List<TransaccionDiariaDTO> obtenerTransaccionesFallback(Throwable ex) {
        log.warn("CB activo para obtenerTransaccionesDiarias: {}", ex.getMessage());
        return Collections.emptyList();
    }

    // -----------------------------------------------------------------------
    // Actualizar saldo (retiro) — publica evento Kafka
    // -----------------------------------------------------------------------

    @Transactional
    @CircuitBreaker(name = "coreService", fallbackMethod = "actualizarSaldoFallback")
    public CuentaInteresDTO actualizarSaldo(Long id, ActualizarSaldoRequest request) {
        CuentaInteres cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + id));

        BigDecimal saldoAnterior = cuenta.getSaldoFinal();
        BigDecimal nuevoSaldo = request.nuevoSaldo();
        cuenta.setSaldoFinal(nuevoSaldo);
        cuentaRepository.save(cuenta);

        BigDecimal montoRetiro = saldoAnterior.subtract(nuevoSaldo);

        RetiroEvent evento = new RetiroEvent(
                cuenta.getCuentaId(),
                montoRetiro,
                saldoAnterior,
                nuevoSaldo,
                Instant.now()
        );
        retiroEventProducer.publicarRetiro(evento);
        log.info("Retiro procesado: cuenta={}, monto={}", id, montoRetiro);

        return mapToCuentaDTO(cuenta);
    }

    public CuentaInteresDTO actualizarSaldoFallback(Long id, ActualizarSaldoRequest request, Throwable ex) {
        log.error("CB activo para actualizarSaldo({}): {}", id, ex.getMessage());
        throw new RuntimeException("Servicio temporalmente no disponible. Intente más tarde.");
    }

    // -----------------------------------------------------------------------
    // Mapeos
    // -----------------------------------------------------------------------

    private CuentaInteresDTO mapToCuentaDTO(CuentaInteres c) {
        return new CuentaInteresDTO(
                c.getCuentaId(),
                c.getNombre(),
                c.getSaldoInicial(),
                c.getSaldoFinal(),
                c.getEdad(),
                c.getTipoCuenta(),
                c.isAnomalia()
        );
    }

    private MovimientoAnualDTO mapToMovimientoDTO(EstadoCuentaAnual m) {
        return new MovimientoAnualDTO(
                m.getCuentaId(),
                m.getFecha(),
                m.getTipoOperacion(),
                m.getMonto(),
                m.getDescripcion(),
                m.isAnomalia()
        );
    }

    private TransaccionDiariaDTO mapToTransaccionDTO(ResumenTransaccion t) {
        return new TransaccionDiariaDTO(
                t.getTransaccionId(),
                t.getFecha(),
                t.getMonto(),
                t.getTipo(),
                t.isAnomalia()
        );
    }
}
