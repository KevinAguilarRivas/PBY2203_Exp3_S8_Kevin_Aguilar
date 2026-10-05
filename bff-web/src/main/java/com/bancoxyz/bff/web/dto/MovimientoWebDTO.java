package com.bancoxyz.bff.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoWebDTO(
        LocalDate fecha,
        String tipoOperacion,
        BigDecimal monto,
        String descripcion,
        boolean anomalia
) {}
