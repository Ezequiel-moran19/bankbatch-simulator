package com.bankbatch.entidad;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTests {

    private Cuenta nuevaCuenta() {
        return new Cuenta(1L, TipoCuenta.CAJA_AHORRO, "CA-0000000001");
    }

    @Test
    void laCuentaNuevaNaceActivaYConSaldoCero() {
        Cuenta cuenta = nuevaCuenta();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void bloquearYDesbloquearCambianElEstado() {
        Cuenta cuenta = nuevaCuenta();

        cuenta.bloquear();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.BLOQUEADA);

        cuenta.desbloquear();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test
    void cerrarExigeSaldoCero() {
        Cuenta cuenta = nuevaCuenta();

        cuenta.cerrar();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.CERRADA);
    }

    @Test
    void unaCuentaCerradaNoCambiaDeEstado() {
        Cuenta cuenta = nuevaCuenta();
        cuenta.cerrar();

        assertThatThrownBy(cuenta::bloquear).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cuenta::desbloquear).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cuenta::cerrar).isInstanceOf(IllegalStateException.class);
    }
}
