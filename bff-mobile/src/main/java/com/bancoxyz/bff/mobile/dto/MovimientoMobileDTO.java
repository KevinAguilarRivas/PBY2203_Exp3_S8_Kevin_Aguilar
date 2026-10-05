package com.bancoxyz.bff.mobile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoMobileDTO(LocalDate fecha, String tipo, BigDecimal monto) {}
