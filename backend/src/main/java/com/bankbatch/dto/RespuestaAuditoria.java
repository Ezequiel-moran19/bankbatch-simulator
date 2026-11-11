package com.bankbatch.dto;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record RespuestaAuditoria(
    Long id,
    Long idUsuario,
    String entidad,
    Long idRegistro,
    String accion,
    JsonNode datosAnteriores,
    JsonNode datosNuevos,
    Instant fecha) {
}
