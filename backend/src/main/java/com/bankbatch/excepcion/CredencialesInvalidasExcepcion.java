package com.bankbatch.excepcion;

public class CredencialesInvalidasExcepcion extends RuntimeException {

    public CredencialesInvalidasExcepcion() {
        super("Credenciales inválidas o usuario no habilitado");
    }
}
