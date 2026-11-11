package com.bankbatch.dto;

import com.bankbatch.entidad.TipoMovimiento;

import java.math.BigDecimal;

public record SolicitudMovimiento(
    String numeroCuenta,
    Long idEjecucion,
    String referencia,
    TipoMovimiento tipoMovimiento,
    BigDecimal importe,
    String descripcion) {
}
