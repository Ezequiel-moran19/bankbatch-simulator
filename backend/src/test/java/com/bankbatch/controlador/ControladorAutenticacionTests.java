package com.bankbatch.controlador;

import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorAutenticacionTests {

    private static final String RUTA_INICIO_SESION = "/api/autenticacion/inicio-sesion";
    private static final String RUTA_PERFIL = "/api/autenticacion/perfil";
    private static final String NOMBRE_USUARIO = "ana_controlador";
    private static final String CONTRASENA = "Clave-segura-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private ServicioJwt servicioJwt;

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario("Ana", "Perez", "ana.controlador@correo.test",
                NOMBRE_USUARIO, codificador.encode(CONTRASENA));
        usuario.agregarRol(repositorioRoles.findByNombre("OPERADOR").orElseThrow());
        return repositorioUsuarios.saveAndFlush(usuario);
    }

    private String cuerpo(String nombreUsuario, String contrasena) {
        return "{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"" + contrasena + "\"}";
    }

    @Test
    void elInicioDeSesionCorrectoDevuelveElToken() throws Exception {
        crearUsuario();

        mockMvc.perform(post(RUTA_INICIO_SESION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(NOMBRE_USUARIO, CONTRASENA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"));
    }

    @Test
    void unaContrasenaIncorrectaDevuelve401() throws Exception {
        crearUsuario();

        mockMvc.perform(post(RUTA_INICIO_SESION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(NOMBRE_USUARIO, "otra-clave")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post(RUTA_INICIO_SESION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("nadie", CONTRASENA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioBloqueadoNoPuedeIniciarSesion() throws Exception {
        Usuario usuario = crearUsuario();
        ReflectionTestUtils.setField(usuario, "estado", EstadoUsuario.BLOQUEADO);
        repositorioUsuarios.saveAndFlush(usuario);

        mockMvc.perform(post(RUTA_INICIO_SESION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(NOMBRE_USUARIO, CONTRASENA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaSolicitudSinCamposObligatoriosDevuelve400() throws Exception {
        mockMvc.perform(post(RUTA_INICIO_SESION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPerfilDevuelveLosDatosDelUsuarioAutenticado() throws Exception {
        Usuario usuario = crearUsuario();
        String token = servicioJwt.emitir(usuario).valor();

        mockMvc.perform(get(RUTA_PERFIL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value(NOMBRE_USUARIO))
                .andExpect(jsonPath("$.correo").value("ana.controlador@correo.test"))
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"));
    }

    @Test
    void elPerfilExigeToken() throws Exception {
        mockMvc.perform(get(RUTA_PERFIL))
                .andExpect(status().isUnauthorized());
    }
}
