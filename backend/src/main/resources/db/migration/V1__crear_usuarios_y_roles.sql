CREATE TABLE usuarios (
    id_usuario      BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    nombre_usuario  VARCHAR(50)  NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso   TIMESTAMPTZ,
    CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT uq_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
);

CREATE TABLE roles (
    id_rol      BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(200),
    CONSTRAINT pk_roles PRIMARY KEY (id_rol),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre)
);

CREATE TABLE usuarios_roles (
    id_usuario BIGINT NOT NULL,
    id_rol     BIGINT NOT NULL,
    CONSTRAINT pk_usuarios_roles PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (id_rol) REFERENCES roles (id_rol)
);

CREATE INDEX ix_usuarios_roles_rol ON usuarios_roles (id_rol);
