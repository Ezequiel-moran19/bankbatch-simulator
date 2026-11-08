package com.bankbatch.repositorio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RestriccionesUsuariosTests {

    private static final String INSERCION =
        "INSERT INTO usuarios (nombre, apellido, correo, nombre_usuario, contrasena_hash) VALUES (?, ?, ?, ?, ?)";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void laBaseRechazaUnNombreDeUsuarioConMayusculas() {
        assertThatThrownBy(() -> jdbc.update(INSERCION, "A", "B", "a@correo.test", "USUARIO_MAYUS", "hash"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_usuarios_nombre_usuario_minusculas");
    }

    @Test
    void laBaseRechazaUnCorreoConMayusculas() {
        assertThatThrownBy(() -> jdbc.update(INSERCION, "A", "B", "A@CORREO.TEST", "usuario_min", "hash"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_usuarios_correo_minusculas");
    }
}
