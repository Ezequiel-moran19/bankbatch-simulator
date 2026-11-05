-- Garantiza que ambos períodos estén informados juntos o sean nulos.
ALTER TABLE ejecuciones_proceso
    DROP CONSTRAINT ck_ejecuciones_proceso_periodo;

ALTER TABLE ejecuciones_proceso
    ADD CONSTRAINT ck_ejecuciones_proceso_periodo
    CHECK (
        (periodo_desde IS NULL AND periodo_hasta IS NULL)
        OR
        (
            periodo_desde IS NOT NULL
            AND periodo_hasta IS NOT NULL
            AND periodo_hasta >= periodo_desde
        )
    );

-- Garantiza la coherencia entre el resultado y los importes.
ALTER TABLE conciliaciones
    DROP CONSTRAINT ck_conciliaciones_presencia;

ALTER TABLE conciliaciones
    ADD CONSTRAINT ck_conciliaciones_presencia
    CHECK (
        (
            resultado = 'COINCIDE'
            AND importe_interno IS NOT NULL
            AND importe_externo IS NOT NULL
            AND importe_interno = importe_externo
        )
        OR
        (
            resultado = 'DIFERENCIA'
            AND importe_interno IS NOT NULL
            AND importe_externo IS NOT NULL
            AND importe_interno <> importe_externo
        )
        OR
        (
            resultado = 'FALTANTE_INTERNO'
            AND importe_interno IS NULL
            AND importe_externo IS NOT NULL
        )
        OR
        (
            resultado = 'FALTANTE_EXTERNO'
            AND importe_interno IS NOT NULL
            AND importe_externo IS NULL
        )
        OR resultado IN ('DUPLICADO_INTERNO', 'DUPLICADO_EXTERNO')
    );
