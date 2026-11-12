-- Datos de demo para probar GET /api/movimientos.
-- Ejecutar con el usuario dueño del esquema (postgres), NO con bankbatch_app.
-- Requiere al menos una cuenta y un usuario existentes.
DO $$
DECLARE
v_cuenta    BIGINT;
    v_ejecucion BIGINT;
BEGIN
SELECT id_cuenta INTO v_cuenta FROM cuentas ORDER BY id_cuenta LIMIT 1;

INSERT INTO ejecuciones_proceso
(id_proceso, id_usuario, nombre_archivo, hash_archivo, estado, fecha_fin,
 total_leidos, total_exitosos)
SELECT p.id_proceso,
       (SELECT id_usuario FROM usuarios ORDER BY id_usuario LIMIT 1),
           'demo_movimientos.csv', repeat('a', 64), 'COMPLETADA', CURRENT_TIMESTAMP, 3, 3
FROM procesos p
WHERE p.codigo = 'ACREDITACION_HABERES'
    RETURNING id_ejecucion INTO v_ejecucion;

INSERT INTO movimientos
(id_cuenta, id_ejecucion, referencia, tipo_movimiento, importe,
 saldo_anterior, saldo_posterior, descripcion)
VALUES
    (v_cuenta, v_ejecucion, 'DEMO-001', 'CREDITO', 1000.00,    0.00, 1000.00, 'Acreditación demo 1'),
    (v_cuenta, v_ejecucion, 'DEMO-002', 'CREDITO',  700.00, 1000.00, 1700.00, 'Acreditación demo 2'),
    (v_cuenta, v_ejecucion, 'DEMO-003', 'DEBITO',   200.00, 1700.00, 1500.00, 'Débito demo');

UPDATE cuentas SET saldo = 1500.00 WHERE id_cuenta = v_cuenta;
END $$;
