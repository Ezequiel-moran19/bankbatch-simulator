package com.bankbatch.servicio;

public enum MotivoRechazoMovimiento {
    CAMPOS_INVALIDOS,
    IMPORTE_INVALIDO,
    CUENTA_INEXISTENTE,
    CUENTA_NO_ACTIVA,
    OPERACION_DUPLICADA,
    SALDO_INSUFICIENTE
}
