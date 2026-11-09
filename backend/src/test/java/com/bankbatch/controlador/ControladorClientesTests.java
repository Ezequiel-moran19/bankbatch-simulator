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
class ControladorClientesTests {

    private static final String RUTA = "/api/clientes";

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

    private String cuerpoAlta(String documento) {
        return "{\"nombre\":\"Ana\",\"apellido\":\"Gomez\",\"documento\":\"" + documento
            + "\",\"correo\":\"ANA.Gomez@Correo.Test\",\"telefono\":\"1155550000\"}";
    }

    private long crearCliente(String token, String documento) throws Exception {
        String respuesta = mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAlta(documento)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private void insertarCuenta(long idCliente, String numero, String estado) {
        jdbc.update("INSERT INTO cuentas (id_cliente, numero_cuenta, tipo_cuenta, estado) "
            + "VALUES (?, ?, 'CAJA_AHORRO', ?)", idCliente, numero, estado);
    }

    @Test
    void elAdministradorCreaUnClienteConDatosNormalizados() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAlta("90000001")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.documento").value("90000001"))
            .andExpect(jsonPath("$.correo").value("ana.gomez@correo.test"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void unDocumentoDuplicadoDevuelve409() throws Exception {
        String token = autenticacion.tokenAdministrador();
        crearCliente(token, "90000002");

        mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAlta("90000002")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estado").value(409))
            .andExpect(jsonPath("$.error").value("Conflicto"));
    }

    @Test
    void losDatosInvalidosDevuelven400ConDetalles() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"apellido\":\"\",\"documento\":\"\",\"correo\":\"no-es-correo\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.length()").value(4));
    }

    @Test
    void unOperadorNoPuedeCrearClientes() throws Exception {
        String token = autenticacion.tokenOperador();

        mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAlta("90000003")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void unOperadorPuedeConsultarClientes() throws Exception {
        String token = autenticacion.tokenOperador();

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void sinTokenSeRechazaConUn401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void laBusquedaFiltraPorTexto() throws Exception {
        String token = autenticacion.tokenAdministrador();
        crearCliente(token, "90000004");

        mockMvc.perform(get(RUTA).param("texto", "gomez").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[?(@.documento=='90000004')]").isNotEmpty());

        mockMvc.perform(get(RUTA).param("texto", "zzz-sin-coincidencias").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void modificarActualizaLosDatosYDejaAuditoria() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000005");

        mockMvc.perform(put(RUTA + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Gomez\",\"correo\":\"ana.maria@correo.test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Ana Maria"))
            .andExpect(jsonPath("$.documento").value("90000005"));

        List<RespuestaAuditoria> registros = servicioAuditoria.consultar(EntidadAuditada.CLIENTE, id);
        assertThat(registros).hasSize(2);
        assertThat(registros.get(0).accion()).isEqualTo("MODIFICACION");
        assertThat(registros.get(0).idUsuario()).isNotNull();
        assertThat(registros.get(0).datosAnteriores().toString()).contains("\"nombre\":\"Ana\"");
        assertThat(registros.get(0).datosNuevos().toString()).contains("\"nombre\":\"Ana Maria\"");
        assertThat(registros.get(1).accion()).isEqualTo("CREACION");
    }

    @Test
    void laBajaDejaAlClienteInactivoYAuditoria() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000006");

        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));

        List<RespuestaAuditoria> registros = servicioAuditoria.consultar(EntidadAuditada.CLIENTE, id);
        assertThat(registros.get(0).accion()).isEqualTo("BAJA_LOGICA");
    }

    @Test
    void noSePuedeDarDeBajaUnClienteConCuentasAbiertas() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000007");
        insertarCuenta(id, "CTA-PRUEBA-0001", "ACTIVA");

        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensaje").value(
                "No se puede dar de baja al cliente: tiene cuentas que no están cerradas"));
    }

    @Test
    void sePuedeDarDeBajaUnClienteConTodasSusCuentasCerradas() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000008");
        insertarCuenta(id, "CTA-PRUEBA-0002", "CERRADA");

        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void noSePuedeDarDeBajaDosVeces() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000009");
        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeModificarUnClienteDadoDeBaja() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long id = crearCliente(token, "90000010");
        mockMvc.perform(post(RUTA + "/" + id + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(put(RUTA + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Otro\",\"apellido\":\"Nombre\",\"correo\":\"otro@correo.test\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void consultarUnClienteInexistenteDevuelve404() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(get(RUTA + "/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    @Test
    void unEstadoInvalidoEnElFiltroDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(get(RUTA).param("estado", "XYZ").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Solicitud incorrecta"));
    }
}
