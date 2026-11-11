package com.bankbatch.dto;

import com.bankbatch.entidad.TipoCuenta;
import jakarta.validation.constraints.NotNull;

public record SolicitudAltaCuenta(
        @NotNull Long idCliente,
        @NotNull TipoCuenta tipoCuenta) {
}
