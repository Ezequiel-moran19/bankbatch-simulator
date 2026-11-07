package com.bankbatch.dto;

import java.time.Instant;
import java.util.List;

public record RespuestaAutenticacion(String token, String tipo, Instant expiraEn, List<String> roles) {
}
