package com.bankbatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudInicioSesion(
        @NotBlank @Size(max = 50) String nombreUsuario,
        @NotBlank @Size(max = 72) String contrasena) {
}
