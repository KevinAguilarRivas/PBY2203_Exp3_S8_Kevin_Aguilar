package com.bancoxyz.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estado_cuenta_anual")
public class EstadoCuentaAnual {
    @Id
    private Long id;
    private Long cuentaId;
    private LocalDate fecha;
    private String tipoOperacion;
    private BigDecimal monto;
    private String descripcion;
    private boolean anomalia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isAnomalia() { return anomalia; }
    public void setAnomalia(boolean anomalia) { this.anomalia = anomalia; }
}
