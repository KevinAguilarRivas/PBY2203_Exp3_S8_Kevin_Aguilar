package com.bancoxyz.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapea la tabla resumen_transacciones_diarias generada por el batch de migracion (Semana 5).
 */
@Entity
@Table(name = "resumen_transacciones_diarias")
public class ResumenTransaccion {
    @Id
    private Long id;
    private Long transaccionId;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
    private boolean anomalia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTransaccionId() { return transaccionId; }
    public void setTransaccionId(Long transaccionId) { this.transaccionId = transaccionId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isAnomalia() { return anomalia; }
    public void setAnomalia(boolean anomalia) { this.anomalia = anomalia; }
}
