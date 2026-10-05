package com.bancoxyz.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaldoEventoDTO implements Serializable {
    private Long cuentaId;
    private String numeroCuenta;
    private String titular;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;
    private String tipoOperacion;
    private String descripcion;
    private LocalDateTime timestamp;
}
