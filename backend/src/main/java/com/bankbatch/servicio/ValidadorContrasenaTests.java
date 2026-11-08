package com.bankbatch.servicio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorContrasenaTests {

    @Test
    void aceptaUnaContrasenaDe72Bytes() {
        assertThat(ValidadorContrasena.excedeElLimite("a".repeat(72))).isFalse();
    }

    @Test
    void rechazaUnaContrasenaDe73Bytes() {
        assertThat(ValidadorContrasena.excedeElLimite("a".repeat(73))).isTrue();
    }

    @Test
    void cuentaBytesYNoCaracteres() {
        assertThat(ValidadorContrasena.excedeElLimite("\u00f1".repeat(36))).isFalse();
        assertThat(ValidadorContrasena.excedeElLimite("\u00f1".repeat(37))).isTrue();
    }
}
