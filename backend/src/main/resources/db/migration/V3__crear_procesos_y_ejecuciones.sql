CREATE TABLE procesos (
    id_proceso     BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo         VARCHAR(50)  NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    descripcion    VARCHAR(300) NOT NULL,
    tipo           VARCHAR(30)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_procesos PRIMARY KEY (id_proceso),
    CONSTRAINT uq_procesos_codigo UNIQUE (codigo),
    CONSTRAINT ck_procesos_tipo CHECK (tipo IN ('ACREDITACION', 'DEBITO', 'CONCILIACION'))
);

CREATE TABLE ejecuciones_proceso (
    id_ejecucion      BIGINT GENERATED ALWAYS AS IDENTITY,
    id_proceso        BIGINT        NOT NULL,
    id_usuario        BIGINT        NOT NULL,
    id_job_execution  BIGINT,
    nombre_archivo    VARCHAR(255)  NOT NULL,
    hash_archivo      VARCHAR(64)   NOT NULL,
    periodo_desde     DATE,
    periodo_hasta     DATE,
    estado            VARCHAR(30)   NOT NULL DEFAULT 'EN_CURSO',
    fecha_inicio      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin         TIMESTAMPTZ,
    total_leidos      INT           NOT NULL DEFAULT 0,
    total_exitosos    INT           NOT NULL DEFAULT 0,
    total_rechazados  INT           NOT NULL DEFAULT 0,
    total_errores     INT           NOT NULL DEFAULT 0,
    observaciones     VARCHAR(500),
    mensaje_error     VARCHAR(1000),
    CONSTRAINT pk_ejecuciones_proceso PRIMARY KEY (id_ejecucion),
    CONSTRAINT fk_ejecuciones_proceso_proceso FOREIGN KEY (id_proceso) REFERENCES procesos (id_proceso),
    CONSTRAINT fk_ejecuciones_proceso_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT ck_ejecuciones_proceso_estado CHECK (estado IN ('EN_CURSO', 'COMPLETADA', 'COMPLETADA_CON_OBSERVACIONES', 'FALLIDA')),
    CONSTRAINT ck_ejecuciones_proceso_hash CHECK (hash_archivo ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_ejecuciones_proceso_periodo CHECK (periodo_hasta >= periodo_desde),
    CONSTRAINT ck_ejecuciones_proceso_contadores CHECK (
        total_leidos >= 0 AND total_exitosos >= 0 AND total_rechazados >= 0 AND total_errores >= 0
    ),
    CONSTRAINT ck_ejecuciones_proceso_invariante CHECK (
        estado = 'EN_CURSO'
        OR total_leidos = total_exitosos + total_rechazados + total_errores
    )
);

CREATE INDEX ix_ejecuciones_proceso_hash ON ejecuciones_proceso (id_proceso, hash_archivo);
CREATE INDEX ix_ejecuciones_proceso_usuario ON ejecuciones_proceso (id_usuario);

CREATE UNIQUE INDEX ux_ejecuciones_proceso_en_curso
    ON ejecuciones_proceso (id_proceso)
    WHERE estado = 'EN_CURSO';
