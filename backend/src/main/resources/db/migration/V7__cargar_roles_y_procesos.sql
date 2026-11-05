INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Acceso completo a la administración del sistema'),
    ('OPERADOR', 'Consulta de información y ejecución de procesos batch');

INSERT INTO procesos (codigo, nombre, descripcion, tipo) VALUES
    ('ACREDITACION_HABERES', 'Acreditación de haberes', 'Acredita haberes en cuentas a partir de un archivo CSV.', 'ACREDITACION'),
    ('DEBITOS_AUTOMATICOS', 'Débitos automáticos', 'Debita importes de cuentas a partir de un archivo CSV, validando saldo suficiente.', 'DEBITO'),
    ('CONCILIACION_BANCARIA', 'Conciliación bancaria', 'Compara los movimientos internos con un archivo externo y registra las diferencias.', 'CONCILIACION');
