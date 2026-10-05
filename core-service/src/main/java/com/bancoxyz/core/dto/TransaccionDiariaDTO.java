package com.bancoxyz.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionDiariaDTO(
        Long transaccionId,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia
) {}
