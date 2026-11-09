package com.bankbatch.excepcion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManejadorExcepcionesTests {

    private static final String RUTA_INICIO_SESION = "/api/autenticacion/inicio-sesion";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unCuerpoSinCamposDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post(RUTA_INICIO_SESION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.estado").value(400))
            .andExpect(jsonPath("$.error").value("Solicitud incorrecta"))
            .andExpect(jsonPath("$.ruta").value(RUTA_INICIO_SESION))
            .andExpect(jsonPath("$.detalles.length()").value(2));
    }

    @Test
    void unCuerpoQueNoEsJsonDevuelve400() throws Exception {
        mockMvc.perform(post(RUTA_INICIO_SESION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.estado").value(400))
            .andExpect(jsonPath("$.error").value("Solicitud incorrecta"));
    }

    @Test
    void lasCredencialesInvalidasDevuelven401ConLaEstructuraComun() throws Exception {
        mockMvc.perform(post(RUTA_INICIO_SESION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"nadie\",\"contrasena\":\"x\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.estado").value(401))
            .andExpect(jsonPath("$.error").value("No autenticado"))
            .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void unaRutaProtegidaSinTokenDevuelve401EnJsonConEncabezadoBearer() throws Exception {
        mockMvc.perform(get("/api/ruta-de-prueba"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", "Bearer"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.estado").value(401))
            .andExpect(jsonPath("$.error").value("No autenticado"))
            .andExpect(jsonPath("$.ruta").value("/api/ruta-de-prueba"));
    }
}
