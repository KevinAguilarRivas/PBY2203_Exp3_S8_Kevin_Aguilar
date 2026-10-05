package com.bancoxyz.bff.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionWebDTO {
    private Long id;
    private String numeroCuenta;
    private String titular;
    private LocalDate fecha;
    private String tipoTransaccion;
    private BigDecimal monto;
    private BigDecimal saldoPostTransaccion;
    private LocalDateTime fechaHora;
    private String descripcion;
    private String referencia;
}
