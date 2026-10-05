package com.bancoxyz.bff.mobile.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CoreMovimientoAnualDTO(
        Long cuentaId, LocalDate fecha,
        String tipoOperacion, BigDecimal monto,
        String descripcion, boolean anomalia
) {}
