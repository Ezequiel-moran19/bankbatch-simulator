package com.bankbatch.configuracion;

import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfiguracionSeguridadTests {

    private static final String RUTA_PROTEGIDA = "/api/ruta-de-prueba";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private ServicioJwt servicioJwt;

    @Autowired
    private JwtEncoder codificador;

    @Autowired
    private JwtDecoder decodificador;

    @Autowired
    private PropiedadesJwt propiedades;

    private Usuario crearUsuarioPersistido() {
        Usuario usuario = new Usuario("Prueba", "Seguridad", "prueba.seguridad@correo.test",
                "prueba_seguridad", "hash");
        Rol rol = repositorioRoles.findByNombre("OPERADOR").orElseThrow();
        usuario.agregarRol(rol);
        return repositorioUsuarios.saveAndFlush(usuario);
    }

    private String encabezado(String token) {
        return "Bearer " + token;
    }

    @Test
    void lasRutasProtegidasExigenToken() throws Exception {
        mockMvc.perform(get(RUTA_PROTEGIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elEstadoDeSaludEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void unTokenValidoDeUnUsuarioActivoPasaLaSeguridad() throws Exception {
        Usuario usuario = crearUsuarioPersistido();
        String token = servicioJwt.emitir(usuario).valor();

        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unTokenDeUnUsuarioBloqueadoEsRechazado() throws Exception {
        Usuario usuario = crearUsuarioPersistido();
        String token = servicioJwt.emitir(usuario).valor();

        ReflectionTestUtils.setField(usuario, "estado", EstadoUsuario.BLOQUEADO);
        repositorioUsuarios.saveAndFlush(usuario);

        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeUnUsuarioInexistenteEsRechazado() throws Exception {
        Usuario fantasma = new Usuario("Fantasma", "Inexistente", "fantasma@correo.test", "fantasma", "hash");
        String token = servicioJwt.emitir(fantasma).valor();

        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenVencidoEsRechazado() throws Exception {
        Usuario usuario = crearUsuarioPersistido();
        Clock haceUnaHora = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        ServicioJwt emisorEnElPasado = new ServicioJwt(codificador, decodificador, propiedades, haceUnaHora);
        String token = emisorEnElPasado.emitir(usuario).valor();

        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtroSecretoEsRechazado() throws Exception {
        Usuario usuario = crearUsuarioPersistido();
        PropiedadesJwt otrasPropiedades =
                new PropiedadesJwt("fedcba9876543210fedcba9876543210", Duration.ofMinutes(15));
        ConfiguracionJwt otraConfiguracion = new ConfiguracionJwt();
        ServicioJwt emisorAjeno = new ServicioJwt(
                otraConfiguracion.codificadorJwt(otrasPropiedades),
                otraConfiguracion.decodificadorJwt(otrasPropiedades),
                otrasPropiedades,
                Clock.systemUTC());
        String token = emisorAjeno.emitir(usuario).valor();

        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenMalformadoEsRechazado() throws Exception {
        mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, encabezado("esto-no-es-un-jwt")))
                .andExpect(status().isUnauthorized());
    }
}
