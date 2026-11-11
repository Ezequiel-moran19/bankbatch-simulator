package com.bankbatch.entidad;

public enum TipoCuenta {
    CAJA_AHORRO("CA"),
    CUENTA_CORRIENTE("CC");

    private final String prefijo;

    TipoCuenta(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
