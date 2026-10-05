package com.bancoxyz.bff.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CuentaWebDTO(
        Long cuentaId,
        String nombre,
        Integer edad,
        String tipoCuenta,
        BigDecimal saldoInicial,
        BigDecimal saldoFinal,
        boolean anomalia,
        List<MovimientoWebDTO> movimientos
) {}
