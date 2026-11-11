package com.bankbatch.excepcion;

public class ConflictoNegocioExcepcion extends RuntimeException {

    public ConflictoNegocioExcepcion(String mensaje) {
        super(mensaje);
    }
}
