CREATE TABLE clientes (
    id_cliente BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    documento  VARCHAR(20)  NOT NULL,
    correo     VARCHAR(150) NOT NULL,
    telefono   VARCHAR(30),
    estado     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    fecha_alta TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uq_clientes_documento UNIQUE (documento),
    CONSTRAINT ck_clientes_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE cuentas (
    id_cuenta      BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente     BIGINT         NOT NULL,
    numero_cuenta  VARCHAR(30)    NOT NULL,
    tipo_cuenta    VARCHAR(30)    NOT NULL,
    saldo          NUMERIC(18,2)  NOT NULL DEFAULT 0,
    version        BIGINT         NOT NULL DEFAULT 0,
    estado         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cuentas PRIMARY KEY (id_cuenta),
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente),
    CONSTRAINT ck_cuentas_tipo CHECK (tipo_cuenta IN ('CAJA_AHORRO', 'CUENTA_CORRIENTE')),
    CONSTRAINT ck_cuentas_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_estado CHECK (estado IN ('ACTIVA', 'BLOQUEADA', 'CERRADA'))
);

CREATE INDEX ix_cuentas_cliente ON cuentas (id_cliente);
