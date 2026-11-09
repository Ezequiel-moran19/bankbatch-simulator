package com.bankbatch.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RespuestasSeguridadJsonTests {

    private final RespuestasSeguridadJson respuestas =
        new RespuestasSeguridadJson(JsonMapper.builder().build());

    @Test
    void laFaltaDeAutenticacionDevuelve401ConEncabezadoBearer() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/protegida");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        respuestas.commence(solicitud, respuesta, new BadCredentialsException("x"));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getContentAsString())
            .contains("\"estado\":401")
            .contains("\"error\":\"No autenticado\"")
            .contains("\"ruta\":\"/api/protegida\"");
    }

    @Test
    void laFaltaDePermisosDevuelve403() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/protegida");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        respuestas.handle(solicitud, respuesta, new AccessDeniedException("x"));

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getContentAsString())
            .contains("\"estado\":403")
            .contains("\"error\":\"Acceso denegado\"")
            .contains("\"ruta\":\"/api/protegida\"");
    }
}
