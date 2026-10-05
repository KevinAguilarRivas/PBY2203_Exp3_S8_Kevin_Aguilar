package com.bancoxyz.bff.cajero.dto;

import java.math.BigDecimal;

public record RetiroResponseDTO(
        Long cuentaId,
        String estado,
        BigDecimal saldoActual,
        String mensaje
) {
    public static RetiroResponseDTO aprobado(Long id, BigDecimal saldo) {
        return new RetiroResponseDTO(id, "APROBADO", saldo, "Retiro procesado exitosamente.");
    }
    public static RetiroResponseDTO rechazado(Long id, String motivo) {
        return new RetiroResponseDTO(id, "RECHAZADO", null, motivo);
    }
}
