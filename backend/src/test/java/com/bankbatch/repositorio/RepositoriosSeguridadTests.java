package com.bankbatch.repositorio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepositoriosSeguridadTests {

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Test
    void existenLosRolesIniciales() {
        assertThat(repositorioRoles.findByNombre("ADMINISTRADOR")).isPresent();
        assertThat(repositorioRoles.findByNombre("OPERADOR")).isPresent();
    }

    @Test
    void noExisteUnUsuarioInexistente() {
        assertThat(repositorioUsuarios.findByNombreUsuario("usuario-que-no-existe")).isEmpty();
    }
}
