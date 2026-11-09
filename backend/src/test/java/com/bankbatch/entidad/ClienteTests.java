package com.bankbatch.entidad;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteTests {

    @Test
    void normalizaLosDatosAlCrearElCliente() {
        Cliente cliente = new Cliente("  Ana ", " Gomez ", " 30111222 ", "  ANA@Correo.Test ", "   ");

        assertThat(cliente.getNombre()).isEqualTo("Ana");
        assertThat(cliente.getApellido()).isEqualTo("Gomez");
        assertThat(cliente.getDocumento()).isEqualTo("30111222");
        assertThat(cliente.getCorreo()).isEqualTo("ana@correo.test");
        assertThat(cliente.getTelefono()).isNull();
        assertThat(cliente.estaActivo()).isTrue();
    }

    @Test
    void laBajaLoDejaInactivo() {
        Cliente cliente = new Cliente("Ana", "Gomez", "30111222", "ana@correo.test", null);

        cliente.darDeBaja();

        assertThat(cliente.getEstado()).isEqualTo(EstadoCliente.INACTIVO);
        assertThat(cliente.estaActivo()).isFalse();
    }
}
