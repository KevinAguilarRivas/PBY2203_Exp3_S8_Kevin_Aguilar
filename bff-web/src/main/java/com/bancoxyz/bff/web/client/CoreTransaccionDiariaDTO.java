package com.bancoxyz.bff.web.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CoreTransaccionDiariaDTO(
        Long transaccionId, LocalDate fecha,
        BigDecimal monto, String tipo, boolean anomalia
) {}
