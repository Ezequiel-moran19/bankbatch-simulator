CREATE TABLE auditoria (
    id_auditoria     BIGINT GENERATED ALWAYS AS IDENTITY,
    id_usuario       BIGINT,
    entidad          VARCHAR(50)  NOT NULL,
    id_registro      BIGINT       NOT NULL,
    accion           VARCHAR(30)  NOT NULL,
    datos_anteriores JSONB,
    datos_nuevos     JSONB,
    fecha_evento     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT ck_auditoria_entidad CHECK (entidad IN ('CLIENTE', 'CUENTA', 'USUARIO', 'PROCESO', 'EJECUCION')),
    CONSTRAINT ck_auditoria_accion CHECK (accion IN ('CREACION', 'MODIFICACION', 'BAJA_LOGICA', 'EJECUCION_PROCESO'))
);

CREATE INDEX ix_auditoria_entidad_registro ON auditoria (entidad, id_registro);
CREATE INDEX ix_auditoria_fecha ON auditoria (fecha_evento);
CREATE INDEX ix_auditoria_usuario ON auditoria (id_usuario);

REVOKE UPDATE, DELETE ON auditoria FROM bankbatch_app;
