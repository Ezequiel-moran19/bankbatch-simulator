package com.bankbatch.dto;

import com.bankbatch.entidad.Movimiento;
import com.bankbatch.entidad.TipoMovimiento;

import java.math.BigDecimal;
import java.time.Instant;

public record RespuestaMovimiento(
    Long id,
    Long idCuenta,
    Long idEjecucion,
    String referencia,
    TipoMovimiento tipoMovimiento,
    BigDecimal importe,
    BigDecimal saldoAnterior,
    BigDecimal saldoPosterior,
    Instant fechaMovimiento,
    String descripcion) {

    public static RespuestaMovimiento desde(Movimiento movimiento) {
        return new RespuestaMovimiento(
            movimiento.getId(),
            movimiento.getIdCuenta(),
            movimiento.getIdEjecucion(),
            movimiento.getReferencia(),
            movimiento.getTipoMovimiento(),
            movimiento.getImporte(),
            movimiento.getSaldoAnterior(),
            movimiento.getSaldoPosterior(),
            movimiento.getFechaMovimiento(),
            movimiento.getDescripcion());
    }
}
