package com.bankbatch.servicio;

import com.bankbatch.configuracion.ConfiguracionJwt;
import com.bankbatch.configuracion.PropiedadesJwt;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioJwtTests {

    private static final String SECRETO = "0123456789abcdef0123456789abcdef";
    private static final String OTRO_SECRETO = "fedcba9876543210fedcba9876543210";

    private ServicioJwt crearServicio(String secreto, Clock reloj) {
        PropiedadesJwt propiedades = new PropiedadesJwt(secreto, Duration.ofMinutes(15));
        ConfiguracionJwt configuracion = new ConfiguracionJwt();
        return new ServicioJwt(
                configuracion.codificadorJwt(propiedades),
                configuracion.decodificadorJwt(propiedades),
                propiedades,
                reloj);
    }

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario("Ana", "Perez", "ana@correo.test", "ana", "hash");
        usuario.agregarRol(new Rol("ADMINISTRADOR", "Administrador de prueba"));
        return usuario;
    }

    @Test
    void emiteUnTokenValidoConLosReclamosEsperados() {
        Clock reloj = Clock.systemUTC();
        ServicioJwt servicio = crearServicio(SECRETO, reloj);

        ServicioJwt.TokenEmitido token = servicio.emitir(crearUsuario());
        Jwt jwt = servicio.validar(token.valor());

        assertThat(jwt.getSubject()).isEqualTo("ana");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMINISTRADOR");
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        assertThat(token.expiraEn()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
        assertThat(token.expiraEn()).isBefore(Instant.now().plus(Duration.ofMinutes(16)));
    }

    @Test
    void rechazaUnTokenVencido() {
        Clock hace1Hora = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        ServicioJwt servicio = crearServicio(SECRETO, hace1Hora);

        ServicioJwt.TokenEmitido token = servicio.emitir(crearUsuario());

        assertThatThrownBy(() -> servicio.validar(token.valor())).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        Clock reloj = Clock.systemUTC();
        ServicioJwt emisor = crearServicio(SECRETO, reloj);
        ServicioJwt validador = crearServicio(OTRO_SECRETO, reloj);

        ServicioJwt.TokenEmitido token = emisor.emitir(crearUsuario());

        assertThatThrownBy(() -> validador.validar(token.valor())).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTextoQueNoEsUnToken() {
        ServicioJwt servicio = crearServicio(SECRETO, Clock.systemUTC());

        assertThatThrownBy(() -> servicio.validar("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }
}
