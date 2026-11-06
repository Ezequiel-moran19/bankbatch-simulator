# Decisiones del modelo de datos

Registro de las decisiones tomadas al implementar las migraciones V1 a V9. Complementa el documento v1.5 y deja constancia de dónde la implementación lo detalla o lo ajusta.

| Tema | Documento v1.5 | Implementación | Motivo |
|---|---|---|---|
| hash_archivo | CHAR(64) | VARCHAR(64) con restricción CHECK | Equivalente en la práctica; el CHECK controla el contenido |
| datos_originales | NVARCHAR(1000) | VARCHAR(1000) | PostgreSQL guarda Unicode en VARCHAR (equivalencia del Anexo B) |
| NOT NULL y defaults | No explícitos | Agregados en las columnas que lo requieren | Integridad de los datos |
| Invariante de contadores | total_leidos = exitosos + rechazados + errores | Se exige solo cuando la ejecución no está EN_CURSO | Durante la ejecución los totales todavía se acumulan |
| Fechas | DATETIME2 → TIMESTAMPTZ (Anexo B) | TIMESTAMPTZ en las tablas de negocio | Guarda el instante exacto; Hibernate trabaja en UTC |
| Fechas de Spring Batch | No aplica | timestamp sin zona | Es el script oficial de Spring Batch 6.0.6 y no se modifica |
| Tablas de Spring Batch | Script oficial como migración Flyway | V6, idéntica al schema-postgresql.sql del JAR de Spring Batch 6.0.6 | Evita divergencias con el motor |
| Códigos de procesos | No definidos | ACREDITACION_HABERES, DEBITOS_AUTOMATICOS, CONCILIACION_BANCARIA (V7) | Aprobados el 05/11/2026; tipos ACREDITACION, DEBITO, CONCILIACION |
| Permisos de bankbatch_app | Auditoría append-only; movimientos históricos | auditoria y movimientos: solo SELECT e INSERT (V5 y V8). Resto: DML completo por defecto | Auditoría y movimientos no se modifican ni eliminan |
| Período de ejecución | Obligatorio solo en conciliación | CHECK: ambos períodos juntos o ambos nulos, con hasta >= desde (V9) | Un CHECK no puede consultar el tipo de proceso; esa regla la valida el servicio |
| Resultados de conciliación | Seis resultados posibles | CHECK de coherencia entre resultado e importes (V9) | Evita filas inconsistentes |
| Administrador inicial | Se carga con BANKBATCH_ADMIN_CLAVE_INICIAL | No hay usuarios ni contraseñas en las migraciones; el mecanismo se define en la Etapa 5 | La contraseña nunca se versiona |

## Verificación

El 05/11/2026 se aplicaron las nueve migraciones sobre una base vacía (bankbatch_prueba_migraciones) con resultado correcto: 9 migraciones exitosas, 2 roles, 3 procesos y permisos de bankbatch_app según lo esperado.

## Reglas

- Una migración ya aplicada no se edita. Toda corrección se resuelve con una migración nueva.
- Los permisos por defecto de cada base se preparan con base-datos/preparar-base.sql.
