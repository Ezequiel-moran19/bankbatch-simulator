package com.bankbatch.servicio;

import com.bankbatch.configuracion.ConfiguracionJwt;
import com.bankbatch.configuracion.PropiedadesJwt;
import com.bankbatch.dto.RespuestaAutenticacion;
import com.bankbatch.dto.SolicitudInicioSesion;
import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.excepcion.CredencialesInvalidasExcepcion;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTests {

    private static final String SECRETO = "0123456789abcdef0123456789abcdef";
    private static final String CONTRASENA = "Clave-segura-1";

    @Mock
    private RepositorioUsuarios repositorioUsuarios;

    private final PasswordEncoder codificador = new BCryptPasswordEncoder();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
    private ServicioAutenticacion servicio;

    @BeforeEach
    void preparar() {
        PropiedadesJwt propiedades = new PropiedadesJwt(SECRETO, Duration.ofMinutes(15));
        ConfiguracionJwt configuracion = new ConfiguracionJwt();
        ServicioJwt servicioJwt = new ServicioJwt(
                configuracion.codificadorJwt(propiedades),
                configuracion.decodificadorJwt(propiedades),
                propiedades,
                reloj);
        servicio = new ServicioAutenticacion(repositorioUsuarios, codificador, servicioJwt, reloj);
    }

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario("Ana", "Perez", "ana@correo.test", "ana", codificador.encode(CONTRASENA));
        usuario.agregarRol(new Rol("OPERADOR", "Operador de prueba"));
        return usuario;
    }

    @Test
    void iniciaSesionConCredencialesCorrectas() {
        Usuario usuario = crearUsuario();
        when(repositorioUsuarios.findByNombreUsuario("ana")).thenReturn(Optional.of(usuario));

        RespuestaAutenticacion respuesta = servicio.iniciarSesion(new SolicitudInicioSesion("ana", CONTRASENA));

        assertThat(respuesta.token()).isNotBlank();
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.roles()).containsExactly("OPERADOR");
        assertThat(respuesta.expiraEn()).isEqualTo(reloj.instant().plus(Duration.ofMinutes(15)));
        assertThat(usuario.getUltimoAcceso()).isEqualTo(reloj.instant());
        verify(repositorioUsuarios).save(usuario);
    }

    @Test
    void normalizaElNombreDeUsuario() {
        Usuario usuario = crearUsuario();
        when(repositorioUsuarios.findByNombreUsuario("ana")).thenReturn(Optional.of(usuario));

        RespuestaAutenticacion respuesta = servicio.iniciarSesion(new SolicitudInicioSesion("  ANA ", CONTRASENA));

        assertThat(respuesta.token()).isNotBlank();
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        Usuario usuario = crearUsuario();
        when(repositorioUsuarios.findByNombreUsuario("ana")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion("ana", "otra-clave")))
                .isInstanceOf(CredencialesInvalidasExcepcion.class);

        assertThat(usuario.getUltimoAcceso()).isNull();
        verify(repositorioUsuarios, never()).save(any());
    }

    @Test
    void rechazaUnUsuarioInexistente() {
        when(repositorioUsuarios.findByNombreUsuario("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion("nadie", CONTRASENA)))
                .isInstanceOf(CredencialesInvalidasExcepcion.class);

        verify(repositorioUsuarios, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoUsuario.class, names = {"INACTIVO", "BLOQUEADO"})
    void rechazaUsuariosQueNoEstanActivos(EstadoUsuario estado) {
        Usuario usuario = crearUsuario();
        ReflectionTestUtils.setField(usuario, "estado", estado);
        when(repositorioUsuarios.findByNombreUsuario("ana")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion("ana", CONTRASENA)))
                .isInstanceOf(CredencialesInvalidasExcepcion.class);

        assertThat(usuario.getUltimoAcceso()).isNull();
        verify(repositorioUsuarios, never()).save(any());
    }
}
