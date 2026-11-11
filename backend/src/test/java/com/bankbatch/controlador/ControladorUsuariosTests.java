package com.bankbatch.controlador;

import com.bankbatch.apoyo.AyudanteAutenticacion;
import com.bankbatch.dto.RespuestaAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioAuditoria;
import com.bankbatch.servicio.ServicioJwt;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bankbatch.apoyo.AyudanteAutenticacion.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorUsuariosTests {

    private static final String RUTA = "/api/usuarios";
    private static final String CONTRASENA = "Clave-prueba-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioAuditoria servicioAuditoria;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private ServicioJwt servicioJwt;

    private AyudanteAutenticacion autenticacion;

    @BeforeEach
    void preparar() {
        autenticacion = new AyudanteAutenticacion(repositorioUsuarios, repositorioRoles, servicioJwt);
    }

    private String cuerpoAlta(String nombreUsuario, String correo, String contrasena, String roles) {
        return "{\"nombre\":\"Olga\",\"apellido\":\"Operadora\",\"correo\":\"" + correo
                + "\",\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"" + contrasena
                + "\",\"roles\":" + roles + "}";
    }

    private long crearUsuario(String token, String nombreUsuario, String correo) throws Exception {
        String respuesta = mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta(nombreUsuario, correo, CONTRASENA, "[\"OPERADOR\"]")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    void elAdministradorCreaUnUsuarioSinExponerLaContrasena() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("Operador.Prueba", "Operador@Correo.Test", CONTRASENA, "[\"OPERADOR\"]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("operador.prueba"))
                .andExpect(jsonPath("$.correo").value("operador@correo.test"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void noSePuedenRepetirNombreDeUsuarioNiCorreo() throws Exception {
        String token = autenticacion.tokenAdministrador();
        crearUsuario(token, "operador.uno", "uno@correo.test");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("operador.uno", "otro@correo.test", CONTRASENA, "[\"OPERADOR\"]")))
                .andExpect(status().isConflict());

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("operador.dos", "uno@correo.test", CONTRASENA, "[\"OPERADOR\"]")))
                .andExpect(status().isConflict());
    }

    @Test
    void losDatosInvalidosDevuelven400() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"apellido\":\"\",\"correo\":\"x\",\"nombreUsuario\":\"a\","
                                + "\"contrasena\":\"corta\",\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(6));
    }

    @Test
    void unRolInexistenteDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("operador.tres", "tres@correo.test", CONTRASENA, "[\"SUPERUSUARIO\"]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaContrasenaDeMasDe72BytesDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();
        String contrasenaLarga = "\u00f1".repeat(40);

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("operador.cuatro", "cuatro@correo.test", contrasenaLarga, "[\"OPERADOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña supera los 72 bytes que admite el cifrado"));
    }

    @Test
    void unOperadorNoPuedeAdministrarUsuarios() throws Exception {
        String token = autenticacion.tokenOperador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("operador.cinco", "cinco@correo.test", CONTRASENA, "[\"OPERADOR\"]")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elUsuarioCreadoPuedeIniciarSesion() throws Exception {
        String token = autenticacion.tokenAdministrador();
        crearUsuario(token, "operador.seis", "seis@correo.test");

        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"operador.seis\",\"contrasena\":\"" + CONTRASENA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"));
    }

    @Test
    void bloquearUnUsuarioLeImpideIniciarSesionYDejaAuditoriaSinDatosSensibles() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearUsuario(token, "operador.siete", "siete@correo.test");

        mockMvc.perform(put(RUTA + "/" + id + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));

        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"operador.siete\",\"contrasena\":\"" + CONTRASENA + "\"}"))
                .andExpect(status().isUnauthorized());

        List<RespuestaAuditoria> registros = servicioAuditoria.consultar(EntidadAuditada.USUARIO, id);
        assertThat(registros).extracting(RespuestaAuditoria::accion).containsExactly("MODIFICACION", "CREACION");
        for (RespuestaAuditoria registro : registros) {
            String datos = String.valueOf(registro.datosAnteriores()) + registro.datosNuevos();
            assertThat(datos).doesNotContain("hash").doesNotContain("contrasena").doesNotContain(CONTRASENA);
        }
    }

    @Test
    void noSePuedeBloquearAlUltimoAdministradorActivo() throws Exception {
        String token = autenticacion.tokenAdministrador();
        jdbc.update("UPDATE usuarios SET estado = 'INACTIVO' WHERE nombre_usuario <> 'administrador_prueba' "
                + "AND id_usuario IN (SELECT ur.id_usuario FROM usuarios_roles ur "
                + "JOIN roles r ON r.id_rol = ur.id_rol WHERE r.nombre = 'ADMINISTRADOR')");
        long idAdministrador = repositorioUsuarios.buscarIdPorNombreUsuario("administrador_prueba").orElseThrow();

        mockMvc.perform(put(RUTA + "/" + idAdministrador + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede dejar al sistema sin un administrador activo"));
    }

    @Test
    void modificarReemplazaLosRolesYLosDatos() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearUsuario(token, "operador.ocho", "ocho@correo.test");

        mockMvc.perform(put(RUTA + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Olga Maria\",\"apellido\":\"Operadora\","
                                + "\"correo\":\"ocho@correo.test\",\"roles\":[\"ADMINISTRADOR\",\"OPERADOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Olga Maria"))
                .andExpect(jsonPath("$.roles.length()").value(2));

        mockMvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR"));
    }

    @Test
    void consultarUnUsuarioInexistenteDevuelve404() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(get(RUTA + "/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinTokenSeRechazaConUn401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laListaFiltraPorTexto() throws Exception {
        String token = autenticacion.tokenAdministrador();
        crearUsuario(token, "operador.nueve", "nueve@correo.test");

        mockMvc.perform(get(RUTA).param("texto", "operador.nueve").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreUsuario").value("operador.nueve"));
    }
}
