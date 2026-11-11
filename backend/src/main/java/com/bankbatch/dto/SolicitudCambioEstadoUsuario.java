package com.bankbatch.dto;

import com.bankbatch.entidad.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record SolicitudCambioEstadoUsuario(@NotNull EstadoUsuario estado) {
}
