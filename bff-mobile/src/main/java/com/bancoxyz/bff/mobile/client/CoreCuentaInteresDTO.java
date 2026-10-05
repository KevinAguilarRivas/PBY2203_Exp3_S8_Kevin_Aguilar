package com.bancoxyz.bff.mobile.client;

import java.math.BigDecimal;

public record CoreCuentaInteresDTO(
        Long cuentaId, String nombre,
        BigDecimal saldoInicial, BigDecimal saldoFinal,
        Integer edad, String tipoCuenta, boolean anomalia
) {}
