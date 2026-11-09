package com.bankbatch.controlador;

import com.bankbatch.apoyo.AyudanteAutenticacion;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioJwt;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.bankbatch.apoyo.AyudanteAutenticacion.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorAuditoriaTests {

    private static final String RUTA = "/api/auditoria";

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void elAdministradorConsultaLaAuditoriaDeUnCliente() throws Exception {
        String token = autenticacion.tokenAdministrador();
        String respuesta = mockMvc.perform(post("/api/clientes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Gomez\",\"documento\":\"90000020\","
                    + "\"correo\":\"ana@correo.test\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        mockMvc.perform(get(RUTA)
                .param("entidad", "CLIENTE")
                .param("idRegistro", String.valueOf(id))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].accion").value("CREACION"))
            .andExpect(jsonPath("$[0].entidad").value("CLIENTE"))
            .andExpect(jsonPath("$[0].datosNuevos.documento").value("90000020"));
    }

    @Test
    void unOperadorNoPuedeConsultarLaAuditoria() throws Exception {
        String token = autenticacion.tokenOperador();

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenSeRechazaConUn401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unaEntidadInvalidaDevuelve400() throws Exception {
        String token = autenticacion.tokenAdministrador();

        mockMvc.perform(get(RUTA).param("entidad", "XYZ").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest());
    }
}
