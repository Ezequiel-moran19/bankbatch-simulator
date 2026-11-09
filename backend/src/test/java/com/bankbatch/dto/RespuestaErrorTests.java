package com.bankbatch.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RespuestaErrorTests {

    private final JsonMapper mapeador = JsonMapper.builder().build();

    @Test
    void conservaTodosLosDatos() {
        Instant fecha = Instant.parse("2026-11-08T12:00:00Z");

        RespuestaError error = new RespuestaError(
            fecha, 400, "Solicitud incorrecta", "Mensaje", "/api/x", List.of("campo: invalido"));

        assertThat(error.fecha()).isEqualTo(fecha);
        assertThat(error.estado()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Solicitud incorrecta");
        assertThat(error.mensaje()).isEqualTo("Mensaje");
        assertThat(error.ruta()).isEqualTo("/api/x");
        assertThat(error.detalles()).containsExactly("campo: invalido");
    }

    @Test
    void seSerializaConLosCamposEsperados() throws Exception {
        RespuestaError error = new RespuestaError(
            Instant.parse("2026-11-08T12:00:00Z"), 401, "No autenticado", "Mensaje", "/api/x", List.of());

        String json = mapeador.writeValueAsString(error);

        assertThat(json)
            .contains("\"fecha\":")
            .contains("\"estado\":401")
            .contains("\"error\":\"No autenticado\"")
            .contains("\"mensaje\":\"Mensaje\"")
            .contains("\"ruta\":\"/api/x\"")
            .contains("\"detalles\":[]");
    }
}
