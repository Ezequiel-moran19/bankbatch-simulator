package com.bankbatch.controlador;

import com.bankbatch.apoyo.AyudanteAutenticacion;
import com.bankbatch.dto.SolicitudMovimiento;
import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.TipoCuenta;
import com.bankbatch.entidad.TipoMovimiento;
import com.bankbatch.repositorio.RepositorioCuentas;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioJwt;
import com.bankbatch.servicio.ServicioMovimientos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.bankbatch.apoyo.AyudanteAutenticacion.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorMovimientosTests {

    private static final String RUTA = "/api/movimientos";
    private static final String NUMERO = "CA-9900000002";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioMovimientos servicioMovimientos;

    @Autowired
    private RepositorioCuentas repositorioCuentas;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private ServicioJwt servicioJwt;

    private AyudanteAutenticacion autenticacion;
    private String token;
    private Long idCuenta;
    private Long idEjecucion;

    @BeforeEach
    void preparar() {
        autenticacion = new AyudanteAutenticacion(repositorioUsuarios, repositorioRoles, servicioJwt);
        token = autenticacion.tokenAdministrador();
        Long idUsuario = repositorioUsuarios.buscarIdPorNombreUsuario("administrador_prueba").orElseThrow();

        idEjecucion = jdbc.queryForObject("""
                INSERT INTO ejecuciones_proceso (id_proceso, id_usuario, nombre_archivo, hash_archivo, estado, fecha_fin)
                SELECT id_proceso, ?, 'prueba.csv', repeat('b', 64), 'COMPLETADA', CURRENT_TIMESTAMP
                FROM procesos WHERE codigo = 'ACREDITACION_HABERES'
                RETURNING id_ejecucion
                """, Long.class, idUsuario);

        Long idCliente = jdbc.queryForObject("""
                INSERT INTO clientes (nombre, apellido, documento, correo)
                VALUES ('Luis', 'Perez', '92000002', 'luis@correo.test')
                RETURNING id_cliente
                """, Long.class);
        idCuenta = repositorioCuentas.saveAndFlush(
            new Cuenta(idCliente, TipoCuenta.CAJA_AHORRO, NUMERO)).getId();

        aplicar("REF-1", TipoMovimiento.CREDITO, "1000.00");
        aplicar("REF-2", TipoMovimiento.CREDITO, "700.00");
        aplicar("REF-3", TipoMovimiento.DEBITO, "200.00");
    }

    private void aplicar(String referencia, TipoMovimiento tipo, String importe) {
        servicioMovimientos.aplicar(new SolicitudMovimiento(
            NUMERO, idEjecucion, referencia, tipo, new BigDecimal(importe), "Prueba"));
    }

    @Test
    void listaLosMovimientosDeUnaCuentaDelMasRecienteAlMasAntiguo() throws Exception {
        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.contenido[0].referencia").value("REF-3"))
            .andExpect(jsonPath("$.contenido[0].tipoMovimiento").value("DEBITO"))
            .andExpect(jsonPath("$.contenido[0].saldoAnterior").value(1700.0))
            .andExpect(jsonPath("$.contenido[0].saldoPosterior").value(1500.0))
            .andExpect(jsonPath("$.contenido[2].referencia").value("REF-1"));
    }

    @Test
    void seFiltraPorTipo() throws Exception {
        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta)).param("tipo", "CREDITO")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2));

        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta)).param("tipo", "DEBITO")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void seFiltraPorEjecucion() throws Exception {
        mockMvc.perform(get(RUTA).param("idEjecucion", String.valueOf(idEjecucion))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    void seFiltraPorRangoDeFechasInclusivo() throws Exception {
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate manana = hoy.plusDays(1);

        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta))
                .param("desde", hoy.toString()).param("hasta", hoy.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3));

        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta))
                .param("desde", manana.toString()).param("hasta", manana.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void laPaginacionRespetaElTamano() throws Exception {
        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta)).param("tamano", "2")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido.length()").value(2))
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void unRangoDeFechasInvertidoDevuelve400() throws Exception {
        mockMvc.perform(get(RUTA).param("desde", "2026-11-30").param("hasta", "2026-11-01")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("La fecha 'hasta' no puede ser anterior a 'desde'"));
    }

    @Test
    void unTipoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(RUTA).param("tipo", "XX")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenSeRechazaConUn401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unOperadorPuedeConsultarMovimientos() throws Exception {
        String tokenOperador = autenticacion.tokenOperador();

        mockMvc.perform(get(RUTA).param("idCuenta", String.valueOf(idCuenta))
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3));
    }
}
