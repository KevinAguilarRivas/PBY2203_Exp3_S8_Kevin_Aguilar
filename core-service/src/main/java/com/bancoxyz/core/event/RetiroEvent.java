package com.bancoxyz.core.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento publicado en Kafka cuando se aprueba y persiste un retiro.
 * El topic bancoxyz.retiros recibe este mensaje, que puede ser consumido
 * por servicios de auditoria, notificaciones u otros BFF.
 */
public record RetiroEvent(
        Long cuentaId,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoNuevo,
        Instant timestamp
) {}
