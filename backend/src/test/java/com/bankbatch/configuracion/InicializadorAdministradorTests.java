package com.bankbatch.configuracion;

import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InicializadorAdministradorTests {

    @Mock
    private RepositorioUsuarios repositorioUsuarios;

    @Mock
    private RepositorioRoles repositorioRoles;

    @Mock
    private PasswordEncoder codificadorContrasenas;

    @Mock
    private ApplicationArguments argumentos;

    private InicializadorAdministrador inicializador;
    private MockEnvironment entorno;

    @BeforeEach
    void configurar() {
        inicializador = new InicializadorAdministrador(
                repositorioUsuarios,
                repositorioRoles,
                codificadorContrasenas);

        entorno = new MockEnvironment();
        inicializador.setEnvironment(entorno);
    }

    @Test
    void debeCrearAdministradorCuandoNoExistenUsuarios() {
        entorno.setProperty("BANKBATCH_ADMIN_CLAVE_INICIAL", "ClaveDePrueba123!");

        Rol rol = new Rol("ADMINISTRADOR", "Acceso completo");

        when(repositorioUsuarios.count()).thenReturn(0L);
        when(repositorioRoles.findByNombre("ADMINISTRADOR"))
                .thenReturn(Optional.of(rol));
        when(codificadorContrasenas.encode("ClaveDePrueba123!"))
                .thenReturn("hash-bcrypt");

        inicializador.run(argumentos);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).save(captor.capture());

        Usuario administrador = captor.getValue();

        assertEquals("admin", administrador.getNombreUsuario());
        assertEquals("admin@bankbatch.local", administrador.getCorreo());
        assertEquals("hash-bcrypt", administrador.getContrasenaHash());
        assertTrue(administrador.estaActivo());
        assertTrue(administrador.getRoles().contains(rol));
    }

    @Test
    void noDebeCrearAdministradorCuandoYaExistenUsuarios() {
        when(repositorioUsuarios.count()).thenReturn(1L);

        inicializador.run(argumentos);

        verify(repositorioUsuarios, never()).save(any(Usuario.class));
        verifyNoInteractions(repositorioRoles, codificadorContrasenas);
    }

    @Test
    void debeImpedirLaCreacionSiFaltaLaContrasena() {
        when(repositorioUsuarios.count()).thenReturn(0L);

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> inicializador.run(argumentos));

        assertTrue(excepcion.getMessage().contains(
                "BANKBATCH_ADMIN_CLAVE_INICIAL"));

        verify(repositorioUsuarios, never()).save(any(Usuario.class));
        verifyNoInteractions(repositorioRoles, codificadorContrasenas);
    }
}
