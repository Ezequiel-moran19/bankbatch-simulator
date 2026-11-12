package com.bankbatch.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Long id;

    @Column(name = "id_cliente", nullable = false, updatable = false)
    private Long idCliente;

    @Column(name = "numero_cuenta", nullable = false, length = 30, updatable = false)
    private String numeroCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuenta", nullable = false, length = 30, updatable = false)
    private TipoCuenta tipoCuenta;

    @Column(name = "saldo", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldo;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private Instant fechaApertura;

    protected Cuenta() {
    }

    public Cuenta(Long idCliente, TipoCuenta tipoCuenta, String numeroCuenta) {
        this.idCliente = idCliente;
        this.tipoCuenta = tipoCuenta;
        this.numeroCuenta = numeroCuenta;
        this.saldo = BigDecimal.ZERO.setScale(2);
        this.estado = EstadoCuenta.ACTIVA;
        this.fechaApertura = Instant.now();
    }

    public void bloquear() {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("Solo una cuenta ACTIVA puede bloquearse");
        }
        this.estado = EstadoCuenta.BLOQUEADA;
    }

    public void desbloquear() {
        if (estado != EstadoCuenta.BLOQUEADA) {
            throw new IllegalStateException("Solo una cuenta BLOQUEADA puede desbloquearse");
        }
        this.estado = EstadoCuenta.ACTIVA;
    }

    public void cerrar() {
        if (estado == EstadoCuenta.CERRADA) {
            throw new IllegalStateException("La cuenta ya está cerrada");
        }
        if (saldo.signum() != 0) {
            throw new IllegalStateException("No se puede cerrar una cuenta con saldo distinto de cero");
        }
        this.estado = EstadoCuenta.CERRADA;
    }

    public void acreditar(BigDecimal importe) {
        validarOperable(importe);
        this.saldo = saldo.add(importe);
    }

    public void debitar(BigDecimal importe) {
        validarOperable(importe);
        if (saldo.compareTo(importe) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        this.saldo = saldo.subtract(importe);
    }

    private void validarOperable(BigDecimal importe) {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("Solo una cuenta ACTIVA admite movimientos");
        }
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public Long getVersion() {
        return version;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public Instant getFechaApertura() {
        return fechaApertura;
    }
}
