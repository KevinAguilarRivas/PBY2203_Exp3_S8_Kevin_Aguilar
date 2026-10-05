package com.bancoxyz.bff.mobile.dto;

import java.math.BigDecimal;

public record ResumenCuentaMobileDTO(
        Long cuentaId,
        String nombre,
        String tipoCuenta,
        BigDecimal saldoActual
) {}
