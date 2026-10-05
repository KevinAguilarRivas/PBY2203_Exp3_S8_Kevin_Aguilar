package com.bancoxyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoAnualDTO(
        Long cuentaId,
        LocalDate fecha,
        String tipoOperacion,
        BigDecimal monto,
        String descripcion,
        boolean anomalia
) {}
