package com.bankbatch.servicio;

import com.bankbatch.entidad.Movimiento;

public record ResultadoMovimiento(Movimiento movimiento, MotivoRechazoMovimiento motivo) {

    public static ResultadoMovimiento exitoso(Movimiento movimiento) {
        return new ResultadoMovimiento(movimiento, null);
    }

    public static ResultadoMovimiento rechazado(MotivoRechazoMovimiento motivo) {
        return new ResultadoMovimiento(null, motivo);
    }

    public boolean aplicado() {
        return motivo == null;
    }
}
