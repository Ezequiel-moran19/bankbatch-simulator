package com.bankbatch.entidad;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class UsuarioTests {
    @Test
    void normalizaElNombreDeUsuarioYElCorreo() {
        Usuario usuario = new Usuario("Ana", "Perez", "  ANA@Correo.Test ", "  ANA  ", "hash");

        assertThat(usuario.getNombreUsuario()).isEqualTo("ana");
        assertThat(usuario.getCorreo()).isEqualTo("ana@correo.test");
    }
}
