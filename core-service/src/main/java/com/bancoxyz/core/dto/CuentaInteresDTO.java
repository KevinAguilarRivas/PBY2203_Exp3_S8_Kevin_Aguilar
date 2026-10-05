package com.bancoxyz.core.dto;

import java.math.BigDecimal;

public record CuentaInteresDTO(
        Long cuentaId,
        String nombre,
        BigDecimal saldoInicial,
        BigDecimal saldoFinal,
        Integer edad,
        String tipoCuenta,
        boolean anomalia
) {}
