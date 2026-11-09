package com.bankbatch.excepcion;

public class CredencialesInvalidasExcepcion extends RuntimeException {

    public CredencialesInvalidasExcepcion() {
        super("Credenciales inválidas");
    }
}
