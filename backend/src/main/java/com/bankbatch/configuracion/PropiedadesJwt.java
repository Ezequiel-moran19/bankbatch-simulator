package com.bankbatch.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "bankbatch.seguridad.jwt")
@Validated
public record PropiedadesJwt(
        @NotBlank
        @Size(min = 32, message = "El secreto JWT debe tener al menos 32 caracteres")
        String secreto,
        @NotNull
        @DefaultValue("15m")
        Duration duracion) {
}
