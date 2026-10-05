package com.bancoxyz.bff.cajero.dto;

import java.math.BigDecimal;

public record SaldoCajeroDTO(Long cuentaId, BigDecimal saldoDisponible) {}
