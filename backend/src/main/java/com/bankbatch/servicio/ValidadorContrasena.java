package com.bankbatch.servicio;

import java.nio.charset.StandardCharsets;

public final class ValidadorContrasena {

    public static final int LIMITE_BYTES_BCRYPT = 72;

    private ValidadorContrasena() {
    }

    public static boolean excedeElLimite(String contrasena) {
        return contrasena.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT;
    }
}
