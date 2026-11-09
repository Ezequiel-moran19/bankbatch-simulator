package com.bankbatch.dto;

import java.time.Instant;
import java.util.List;

public record RespuestaError(Instant fecha, int estado, String error, String mensaje, String ruta, List<String> detalles) {
}
