package com.bankbatch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudModificacionCliente(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido,
    @NotBlank @Email @Size(max = 150) String correo,
    @Size(max = 30) String telefono) {
}
