package com.bankbatch.dto;

import java.time.Instant;
import java.util.List;

public record RespuestaPerfil(
        String nombreUsuario,
        String nombre,
        String apellido,
        String correo,
        List<String> roles,
        Instant ultimoAcceso) {
}
