package com.bankbatch.dto;

import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.EstadoCuenta;
import com.bankbatch.entidad.TipoCuenta;

import java.math.BigDecimal;
import java.time.Instant;

public record RespuestaCuenta(
        Long id,
        Long idCliente,
        String numeroCuenta,
        TipoCuenta tipoCuenta,
        BigDecimal saldo,
        EstadoCuenta estado,
        Instant fechaApertura) {

    public static RespuestaCuenta desde(Cuenta cuenta) {
        return new RespuestaCuenta(
                cuenta.getId(),
                cuenta.getIdCliente(),
                cuenta.getNumeroCuenta(),
                cuenta.getTipoCuenta(),
                cuenta.getSaldo(),
                cuenta.getEstado(),
                cuenta.getFechaApertura());
    }
}
