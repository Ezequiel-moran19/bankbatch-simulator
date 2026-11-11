package com.bankbatch.dto;

import com.bankbatch.entidad.NombreRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SolicitudAltaUsuario(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$") String nombreUsuario,
        @NotBlank @Size(min = 10, max = 72) String contrasena,
        @NotEmpty Set<NombreRol> roles) {
}
