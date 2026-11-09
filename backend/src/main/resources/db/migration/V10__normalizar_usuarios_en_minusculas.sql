ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_nombre_usuario_minusculas
        CHECK (nombre_usuario = lower(nombre_usuario));

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_correo_minusculas
        CHECK (correo = lower(correo));
