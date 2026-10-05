package com.bancoxyz.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cuentas_interes")
public class CuentaInteres {
    @Id
    private Long id;
    private Long cuentaId;
    private String nombre;
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;
    private Integer edad;
    private String tipoCuenta;
    private boolean anomalia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    public boolean isAnomalia() { return anomalia; }
    public void setAnomalia(boolean anomalia) { this.anomalia = anomalia; }
}
