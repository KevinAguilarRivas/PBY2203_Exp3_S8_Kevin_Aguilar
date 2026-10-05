package com.bancoxyz.bff.web.client;

import java.math.BigDecimal;

public record CoreCuentaInteresDTO(
        Long cuentaId, String nombre,
        BigDecimal saldoInicial, BigDecimal saldoFinal,
        Integer edad, String tipoCuenta, boolean anomalia
) {}
