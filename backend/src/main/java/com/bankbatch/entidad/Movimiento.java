package com.bankbatch.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Immutable
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @Column(name = "id_cuenta", nullable = false, updatable = false)
    private Long idCuenta;

    @Column(name = "id_ejecucion", nullable = false, updatable = false)
    private Long idEjecucion;

    @Column(name = "referencia", nullable = false, length = 100, updatable = false)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 30, updatable = false)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "importe", nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal importe;

    @Column(name = "saldo_anterior", nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal saldoPosterior;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private Instant fechaMovimiento;

    @Column(name = "descripcion", nullable = false, length = 250, updatable = false)
    private String descripcion;

    protected Movimiento() {
    }

    public Movimiento(Long idCuenta, Long idEjecucion, String referencia, TipoMovimiento tipoMovimiento,
                      BigDecimal importe, BigDecimal saldoAnterior, BigDecimal saldoPosterior, String descripcion) {
        this.idCuenta = idCuenta;
        this.idEjecucion = idEjecucion;
        this.referencia = referencia;
        this.tipoMovimiento = tipoMovimiento;
        this.importe = importe;
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoPosterior;
        this.descripcion = descripcion;
        this.fechaMovimiento = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getIdCuenta() {
        return idCuenta;
    }

    public Long getIdEjecucion() {
        return idEjecucion;
    }

    public String getReferencia() {
        return referencia;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public BigDecimal getSaldoAnterior() {
        return saldoAnterior;
    }

    public BigDecimal getSaldoPosterior() {
        return saldoPosterior;
    }

    public Instant getFechaMovimiento() {
        return fechaMovimiento;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
