CREATE TABLE movimientos (
    id_movimiento    BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cuenta        BIGINT        NOT NULL,
    id_ejecucion     BIGINT        NOT NULL,
    referencia       VARCHAR(100)  NOT NULL,
    tipo_movimiento  VARCHAR(30)   NOT NULL,
    importe          NUMERIC(18,2) NOT NULL,
    saldo_anterior   NUMERIC(18,2) NOT NULL,
    saldo_posterior  NUMERIC(18,2) NOT NULL,
    fecha_movimiento TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion      VARCHAR(250)  NOT NULL,
    CONSTRAINT pk_movimientos PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_movimientos_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas (id_cuenta),
    CONSTRAINT fk_movimientos_ejecucion FOREIGN KEY (id_ejecucion) REFERENCES ejecuciones_proceso (id_ejecucion),
    CONSTRAINT uq_movimientos_cuenta_referencia_tipo UNIQUE (id_cuenta, referencia, tipo_movimiento),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo_movimiento IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_movimientos_importe CHECK (importe > 0),
    CONSTRAINT ck_movimientos_saldos CHECK (saldo_anterior >= 0 AND saldo_posterior >= 0),
    CONSTRAINT ck_movimientos_coherencia CHECK (
        (tipo_movimiento = 'CREDITO' AND saldo_posterior = saldo_anterior + importe)
        OR (tipo_movimiento = 'DEBITO' AND saldo_posterior = saldo_anterior - importe)
    )
);

CREATE INDEX ix_movimientos_cuenta_fecha ON movimientos (id_cuenta, fecha_movimiento);
CREATE INDEX ix_movimientos_ejecucion ON movimientos (id_ejecucion);

CREATE TABLE registros_procesados (
    id_registro         BIGINT GENERATED ALWAYS AS IDENTITY,
    id_ejecucion        BIGINT        NOT NULL,
    numero_linea        INT           NOT NULL,
    referencia          VARCHAR(100),
    datos_originales    VARCHAR(1000) NOT NULL,
    estado              VARCHAR(30)   NOT NULL,
    motivo              VARCHAR(500),
    fecha_procesamiento TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_registros_procesados PRIMARY KEY (id_registro),
    CONSTRAINT fk_registros_procesados_ejecucion FOREIGN KEY (id_ejecucion) REFERENCES ejecuciones_proceso (id_ejecucion),
    CONSTRAINT ck_registros_procesados_linea CHECK (numero_linea > 0),
    CONSTRAINT ck_registros_procesados_estado CHECK (estado IN ('PROCESADO', 'RECHAZADO', 'ERROR')),
    CONSTRAINT ck_registros_procesados_motivo CHECK (
        (estado = 'PROCESADO' AND motivo IS NULL)
        OR (estado <> 'PROCESADO' AND motivo IS NOT NULL)
    )
);

CREATE INDEX ix_registros_procesados_ejecucion ON registros_procesados (id_ejecucion, estado);

CREATE TABLE conciliaciones (
    id_conciliacion  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_ejecucion     BIGINT        NOT NULL,
    numero_cuenta    VARCHAR(30)   NOT NULL,
    referencia       VARCHAR(100)  NOT NULL,
    tipo_movimiento  VARCHAR(30)   NOT NULL,
    importe_interno  NUMERIC(18,2),
    importe_externo  NUMERIC(18,2),
    resultado        VARCHAR(30)   NOT NULL,
    observacion      VARCHAR(300),
    CONSTRAINT pk_conciliaciones PRIMARY KEY (id_conciliacion),
    CONSTRAINT fk_conciliaciones_ejecucion FOREIGN KEY (id_ejecucion) REFERENCES ejecuciones_proceso (id_ejecucion),
    CONSTRAINT ck_conciliaciones_tipo CHECK (tipo_movimiento IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_conciliaciones_resultado CHECK (resultado IN (
        'COINCIDE', 'DIFERENCIA', 'FALTANTE_INTERNO', 'FALTANTE_EXTERNO',
        'DUPLICADO_INTERNO', 'DUPLICADO_EXTERNO'
    )),
    CONSTRAINT ck_conciliaciones_importes CHECK (
        (importe_interno IS NULL OR importe_interno > 0)
        AND (importe_externo IS NULL OR importe_externo > 0)
    ),
    CONSTRAINT ck_conciliaciones_presencia CHECK (
        (resultado IN ('COINCIDE', 'DIFERENCIA') AND importe_interno IS NOT NULL AND importe_externo IS NOT NULL)
        OR (resultado = 'FALTANTE_INTERNO' AND importe_interno IS NULL AND importe_externo IS NOT NULL)
        OR (resultado = 'FALTANTE_EXTERNO' AND importe_interno IS NOT NULL AND importe_externo IS NULL)
        OR resultado IN ('DUPLICADO_INTERNO', 'DUPLICADO_EXTERNO')
    )
);

CREATE INDEX ix_conciliaciones_ejecucion ON conciliaciones (id_ejecucion, resultado);
