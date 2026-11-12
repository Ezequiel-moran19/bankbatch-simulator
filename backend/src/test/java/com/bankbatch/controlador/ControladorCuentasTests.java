package com.bankbatch.controlador;

import com.bankbatch.apoyo.AyudanteAutenticacion;
import com.bankbatch.dto.RespuestaAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioAuditoria;
import com.bankbatch.servicio.ServicioJwt;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorCuentasTests {

    private static final String RUTA = "/api/cuentas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

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

    private long crearCliente(String token, String documento) throws Exception {
        String respuesta = mockMvc.perform(post("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Gomez\",\"documento\":\"" + documento
                                + "\",\"correo\":\"ana@correo.test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String cuerpoCuenta(long idCliente, String tipo) {
        return "{\"idCliente\":" + idCliente + ",\"tipoCuenta\":\"" + tipo + "\"}";
    }

    private String crearCuentaYObtenerRespuesta(String token, long idCliente, String tipo) throws Exception {
        return mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCuenta(idCliente, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long crearCuenta(String token, long idCliente) throws Exception {
        String respuesta = crearCuentaYObtenerRespuesta(token, idCliente, "CAJA_AHORRO");
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    void laCuentaSeCreaConNumeroGeneradoYSaldoCero() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000001");

        String respuesta = crearCuentaYObtenerRespuesta(token, idCliente, "CAJA_AHORRO");

        assertThat((String) JsonPath.read(respuesta, "$.numeroCuenta")).matches("CA-\\d{10}");
        assertThat(((Number) JsonPath.read(respuesta, "$.saldo")).doubleValue()).isZero();
        assertThat((String) JsonPath.read(respuesta, "$.estado")).isEqualTo("ACTIVA");
        assertThat(((Number) JsonPath.read(respuesta, "$.idCliente")).longValue()).isEqualTo(idCliente);
    }

    @Test
    void unaCuentaCorrienteUsaElPrefijoCC() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000002");

        String respuesta = crearCuentaYObtenerRespuesta(token, idCliente, "CUENTA_CORRIENTE");

        assertThat((String) JsonPath.read(respuesta, "$.numeroCuenta")).matches("CC-\\d{10}");
    }

    @Test
    void noSePuedeAbrirUnaCuentaParaUnClienteInexistente() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCuenta(999999999L, "CAJA_AHORRO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void noSePuedeAbrirUnaCuentaParaUnClienteDadoDeBaja() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000003");
        mockMvc.perform(post("/api/clientes/" + idCliente + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCuenta(idCliente, "CAJA_AHORRO")))
                .andExpect(status().isConflict());
    }

    @Test
    void unTipoDeCuentaInvalidoDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000004");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCuenta(idCliente, "INEXISTENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("CAJA_AHORRO")));
    }

    @Test
    void unaSolicitudSinClienteDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoCuenta\":\"CAJA_AHORRO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(1));
    }

    @Test
    void unOperadorNoPuedeCrearCuentasPeroSiConsultarlas() throws Exception {
        String token = autenticacion.tokenOperador();

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCuenta(1L, "CAJA_AHORRO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void laListaSeFiltraPorCliente() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000005");
        crearCuenta(token, idCliente);
        crearCuenta(token, idCliente);

        mockMvc.perform(get(RUTA).param("idCliente", String.valueOf(idCliente))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void bloquearYDesbloquearCambianElEstadoYDejanAuditoria() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCuenta = crearCuenta(token, crearCliente(token, "91000006"));

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/bloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
        mockMvc.perform(post(RUTA + "/" + idCuenta + "/desbloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        List<RespuestaAuditoria> registros = servicioAuditoria.consultar(EntidadAuditada.CUENTA, idCuenta);
        assertThat(registros).extracting(RespuestaAuditoria::accion)
                .containsExactly("MODIFICACION", "MODIFICACION", "CREACION");
    }

    @Test
    void noSePuedeBloquearDosVecesNiDesbloquearUnaCuentaActiva() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCuenta = crearCuenta(token, crearCliente(token, "91000007"));

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/desbloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/bloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post(RUTA + "/" + idCuenta + "/bloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void cerrarUnaCuentaSinSaldoLaDejaCerradaYAudita() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCuenta = crearCuenta(token, crearCliente(token, "91000008"));

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        List<RespuestaAuditoria> registros = servicioAuditoria.consultar(EntidadAuditada.CUENTA, idCuenta);
        assertThat(registros.get(0).accion()).isEqualTo("BAJA_LOGICA");
    }

    @Test
    void noSePuedeCerrarUnaCuentaConSaldo() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCuenta = crearCuenta(token, crearCliente(token, "91000009"));
        jdbc.update("UPDATE cuentas SET saldo = 100 WHERE id_cuenta = ?", idCuenta);
        entityManager.clear();

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede cerrar una cuenta con saldo distinto de cero"));
    }

    @Test
    void unaCuentaCerradaNoPuedeBloquearse() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCuenta = crearCuenta(token, crearCliente(token, "91000010"));
        mockMvc.perform(post(RUTA + "/" + idCuenta + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/bloqueo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void elClienteSoloPuedeDarseDeBajaCuandoSusCuentasEstanCerradas() throws Exception {
        String token = autenticacion.tokenAdministrador();
        long idCliente = crearCliente(token, "91000011");
        long idCuenta = crearCuenta(token, idCliente);

        mockMvc.perform(post("/api/clientes/" + idCliente + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());

        mockMvc.perform(post(RUTA + "/" + idCuenta + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/clientes/" + idCliente + "/baja").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void consultarUnaCuentaInexistenteDevuelve404() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(get(RUTA + "/999999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinTokenSeRechazaConUn401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }
}
