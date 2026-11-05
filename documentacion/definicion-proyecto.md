# BankBatch Simulator

**Documento de definición y planificación del proyecto**

**Versión:** 1.5 (definitiva, base para iniciar el desarrollo; motor PostgreSQL) **Tipo:** Aplicación web Full Stack **Dominio:** Simulación de procesos batch bancarios con datos ficticios **Finalidad:** Proyecto de portfolio profesional y demostración de conocimientos de desarrollo, bases de datos y testing

Este documento consolida y reemplaza las versiones 1.0 a 1.4. Las decisiones tomadas durante la consolidación figuran en el Anexo A.

---

## 1. Descripción general

BankBatch Simulator es una aplicación web que simula la administración y ejecución de procesos batch propios de un entorno bancario ficticio. Un proceso batch procesa un conjunto de registros de forma automática, aplicando reglas de negocio y dejando evidencia de los resultados.

El sistema permite administrar clientes y cuentas, consultar movimientos, cargar archivos CSV, ejecutar procesos manualmente, consultar sus resultados y analizar registros procesados, rechazados o fallidos.

Tiene dos partes:

- **Frontend:** interfaz administrativa web.
- **Backend:** API REST con reglas de negocio, seguridad, persistencia y ejecución de procesos batch.

No es un banco digital ni un sistema productivo. No usa dinero real ni se conecta con entidades financieras.

### Objetivos

- Aplicación Full Stack con Angular y Java.
- Procesamiento por lotes con Spring Batch.
- Persistencia relacional en PostgreSQL con migraciones Flyway.
- API REST documentada.
- Transacciones, control de concurrencia e idempotencia.
- Autenticación y autorización con JWT.
- Auditoría y trazabilidad.
- Pruebas unitarias, de integración, de API y E2E.
- Integración continua con GitHub Actions.

## 2. Alcance

**Incluido en la versión 1.0:** autenticación y autorización; gestión de usuarios (solo ADMINISTRADOR), clientes y cuentas; consulta de movimientos; acreditación de haberes; débitos automáticos; conciliación bancaria; procesamiento de CSV; registro y trazabilidad de ejecuciones; reportes de procesamiento (resumen por ejecución con totales, duración y rechazos por motivo, sin exportación); consulta de rechazos; detección de duplicados; idempotencia; bloqueo optimista; restricción de ejecuciones simultáneas del mismo proceso; auditoría; panel administrativo; testing en todos los niveles; GitHub Actions.

**Fuera de alcance:** bancos reales, transferencias, tarjetas, préstamos, inversiones, scoring, impuestos, microservicios, Kafka, Kubernetes, arquitectura distribuida o multiinstancia, infraestructura cloud compleja, planificación automática (cron), cancelación manual de Jobs, refresh tokens, aplicaciones móviles y notificaciones externas.

## 3. Stack tecnológico

| Componente | Tecnología |
| --- | --- |
| Frontend | Angular, TypeScript, Bootstrap 5.3, Bootstrap Icons, HttpClient |
| Backend | Java 21, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Maven |
| Procesamiento | Spring Batch |
| Seguridad | Spring Security, JWT, BCrypt |
| Base de datos | PostgreSQL 18, instalación local (no se usa Supabase) |
| Migraciones | Flyway |
| Documentación API | Swagger/OpenAPI |
| Testing | JUnit 5, Mockito, Postman, Playwright |
| Versionado y CI | Git, GitHub, GitHub Actions |

Al iniciar se fijarán versiones estables y compatibles entre sí. La versión de Spring Batch condiciona el script de metadatos (sección 9.4).

## 4. Arquitectura

Arquitectura cliente-servidor. El backend es un monolito modular en capas; no hay microservicios. Angular nunca accede a la base de datos.

```text
USUARIO -> FRONTEND ANGULAR -> HTTP/JSON -> API REST (Spring Boot)
                                              |
                        controladores -> servicios -> repositorios
                                              |
                                        SPRING BATCH
                                 lector -> procesador -> escritor
                                              |
                                         POSTGRESQL
```

### Organización del backend

Nombres de clases, atributos, métodos, variables y tablas en español, salvo nombres reservados u obligatorios de Java, Spring o sus bibliotecas.

```text
backend/
+-- src/main/java/com/bankbatch/
|   +-- configuracion/   (ConfiguracionSeguridad, ConfiguracionBatch)
|   +-- controlador/     (Autenticacion, Usuarios, Clientes, Cuentas, Movimientos,
|   |                     Procesos, Ejecuciones, Conciliaciones, Dashboard, Auditoria)
|   +-- servicio/        (un servicio por recurso, más ServicioAutenticacion)
|   +-- repositorio/     (un repositorio por entidad)
|   +-- entidad/         (Usuario, Rol, Cliente, Cuenta, Movimiento, Proceso,
|   |                     EjecucionProceso, RegistroProcesado, Conciliacion, Auditoria)
|   +-- dto/
|   +-- excepcion/       (ManejadorExcepciones)
|   +-- batch/
|       +-- acreditacion/  (Trabajo, Lector, Procesador, Escritor)
|       +-- debitos/       (Trabajo, Lector, Procesador, Escritor)
|       +-- conciliacion/  (Trabajo, Lector, Procesador, Escritor)
+-- src/main/resources/ (application.yml, db/migration/)
+-- src/test/
```

| Capa | Responsabilidad |
| --- | --- |
| configuracion | Seguridad, Batch y componentes generales |
| controlador | Solicitudes HTTP y respuestas REST |
| servicio | Reglas de negocio y coordinación |
| repositorio | Acceso a datos |
| entidad | Entidades persistentes |
| dto | Objetos de transferencia |
| excepcion | Manejo centralizado de errores |
| batch | Jobs, Steps, lectores, procesadores y escritores |

## 5. Seguridad

### 5.1. Autenticación

- Login con usuario y contraseña; contraseñas con BCrypt, nunca en texto plano.
- JWT con duración de **15 minutos**. Sin refresh token en la v1.0: al expirar, el usuario inicia sesión de nuevo.
- La clave de firma se toma de una variable de entorno. Nunca se versiona.
- Los archivos locales con secretos se excluyen con `.gitignore`.
- El usuario con estado distinto de ACTIVO no puede iniciar sesión. El filtro JWT consulta el estado del usuario en cada solicitud, de modo que un bloqueo surte efecto de inmediato y no recién al expirar el token. Se registra `ultimo_acceso` en cada ingreso correcto.
- El bloqueo de usuarios es manual (ADMINISTRADOR). No hay bloqueo automático por intentos fallidos en la v1.0.
- La contraseña inicial del administrador de desarrollo se carga con un script que toma el valor de una variable de entorno; no se versiona.
- CORS restringido al origen del frontend.

### 5.2. Autorización por rol

El control definitivo se realiza en el backend. El frontend solo oculta opciones.

| Funcionalidad | ADMINISTRADOR | OPERADOR |
| --- | --- | --- |
| Dashboard, consulta de clientes, cuentas y movimientos | Sí | Sí |
| Alta, modificación y baja lógica de clientes y cuentas | Sí | No |
| Gestión de usuarios y roles | Sí | No |
| Consulta de procesos, ejecuciones, registros y conciliaciones | Sí | Sí |
| Ejecutar procesos batch | Sí | Sí |
| Consulta de auditoría | Sí | No |

### 5.3. Protección de datos

- No exponer contraseñas, hashes ni información interna innecesaria.
- Validar tipo, tamaño y estructura de los archivos recibidos.
- No registrar contraseñas, hashes, tokens ni secretos en auditoría ni en logs.
- Los errores al usuario no revelan detalles de infraestructura.

## 6. Modelo de datos

PostgreSQL 18 (ver Anexo B para las equivalencias de tipos). Todo el esquema se crea y versiona únicamente con Flyway. Las fechas se almacenan en UTC (`DATETIME2`) y el frontend las presenta en hora local (America/Argentina/Buenos\_Aires). Las fechas de período de la conciliación se interpretan en esa zona horaria y se convierten a UTC para filtrar. Todas las claves primarias (salvo la compuesta de usuarios\_roles) son `BIGINT IDENTITY`.

### 6.1. usuarios, roles, usuarios\_roles

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_usuario | BIGINT | PK |
| nombre, apellido | VARCHAR(100) |  |
| correo | VARCHAR(150) | UNIQUE |
| nombre\_usuario | VARCHAR(50) | UNIQUE |
| contrasena\_hash | VARCHAR(255) | Hash BCrypt |
| estado | VARCHAR(20) | CHECK: ACTIVO, INACTIVO, BLOQUEADO |
| fecha\_creacion | DATETIME2 |  |
| ultimo\_acceso | DATETIME2 | NULL |

`roles` (id\_rol, nombre UNIQUE, descripcion) con ADMINISTRADOR y OPERADOR. `usuarios_roles` (id\_usuario, id\_rol) con PK compuesta y FK a ambas tablas; relación muchos a muchos.

### 6.2. clientes

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_cliente | BIGINT | PK |
| nombre, apellido | VARCHAR(100) |  |
| documento | VARCHAR(20) | UNIQUE |
| correo | VARCHAR(150) |  |
| telefono | VARCHAR(30) | NULL |
| estado | VARCHAR(20) | CHECK: ACTIVO, INACTIVO (baja lógica) |
| fecha\_alta | DATETIME2 |  |

No se elimina físicamente un cliente con cuentas o movimientos.

### 6.3. cuentas

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_cuenta | BIGINT | PK |
| id\_cliente | BIGINT | FK clientes |
| numero\_cuenta | VARCHAR(30) | UNIQUE |
| tipo\_cuenta | VARCHAR(30) | CHECK: CAJA\_AHORRO, CUENTA\_CORRIENTE |
| saldo | DECIMAL(18,2) | CHECK (saldo >= 0): sin sobregiro en la v1.0 |
| version | BIGINT | `@Version` (bloqueo optimista) |
| estado | VARCHAR(20) | CHECK: ACTIVA, BLOQUEADA, CERRADA (CERRADA = baja lógica) |
| fecha\_apertura | DATETIME2 |  |

El saldo solo se modifica mediante operaciones controladas por las reglas de negocio (procesos batch), nunca por una edición genérica. La baja lógica (CERRADA) exige saldo igual a cero. Un cliente solo pasa a INACTIVO si todas sus cuentas están CERRADAS.

### 6.4. procesos

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_proceso | BIGINT | PK |
| codigo | VARCHAR(50) | UNIQUE |
| nombre | VARCHAR(100) |  |
| descripcion | VARCHAR(300) |  |
| tipo | VARCHAR(30) | CHECK: ACREDITACION, DEBITO, CONCILIACION |
| activo | BIT |  |
| fecha\_creacion | DATETIME2 |  |

### 6.5. ejecuciones\_proceso

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_ejecucion | BIGINT | PK |
| id\_proceso | BIGINT | FK procesos |
| id\_usuario | BIGINT | FK usuarios: quién la solicitó |
| id\_job\_execution | BIGINT | NULL; vínculo con `BATCH_JOB_EXECUTION` |
| nombre\_archivo | VARCHAR(255) |  |
| hash\_archivo | CHAR(64) | SHA-256 hexadecimal |
| periodo\_desde, periodo\_hasta | DATE | NULL; obligatorios solo en conciliación, con periodo\_hasta >= periodo\_desde |
| estado | VARCHAR(30) | CHECK (ver abajo) |
| fecha\_inicio | DATETIME2 |  |
| fecha\_fin | DATETIME2 | NULL |
| total\_leidos | INT | Filas leídas |
| total\_exitosos | INT |  |
| total\_rechazados | INT | Rechazos funcionales |
| total\_errores | INT | Errores técnicos por fila |
| observaciones | VARCHAR(500) | NULL |
| mensaje\_error | VARCHAR(1000) | NULL |

Invariante: `total_leidos = total_exitosos + total_rechazados + total_errores`. Índice por `(id_proceso, hash_archivo)`.

**Estados funcionales:** `EN_CURSO`, `COMPLETADA`, `COMPLETADA_CON_OBSERVACIONES`, `FALLIDA`. Controlados con enumeración Java y `CHECK`. No existe `CANCELADA` en la v1.0.

- Toda ejecución nace en EN\_CURSO, con la fila creada antes de lanzar el Job para conservar evidencia aunque el Job no llegue a iniciar. Un archivo inválido o ya procesado se rechaza con error 4xx antes de crear la ejecución; una falla posterior a la creación y previa al lanzamiento deja la ejecución en FALLIDA.
- Los estados terminales no se modifican.

### 6.6. movimientos

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_movimiento | BIGINT | PK |
| id\_cuenta | BIGINT | FK cuentas |
| id\_ejecucion | BIGINT | FK ejecuciones\_proceso (todo movimiento nace de un proceso) |
| referencia | VARCHAR(100) |  |
| tipo\_movimiento | VARCHAR(30) | CHECK: CREDITO, DEBITO |
| importe | DECIMAL(18,2) | CHECK (importe > 0) |
| saldo\_anterior, saldo\_posterior | DECIMAL(18,2) |  |
| fecha\_movimiento | DATETIME2 |  |
| descripcion | VARCHAR(250) |  |

`UNIQUE (id_cuenta, referencia, tipo_movimiento)`. La referencia no es globalmente única. Los movimientos son históricos: no se modifican ni eliminan. Índice por `(id_cuenta, fecha_movimiento)`. El saldo inicial de las cuentas de prueba se carga por script de datos iniciales, sin movimiento asociado.

### 6.7. registros\_procesados

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_registro | BIGINT | PK |
| id\_ejecucion | BIGINT | FK |
| numero\_linea | INT | Línea del archivo |
| referencia | VARCHAR(100) | NULL si la fila es ilegible |
| datos\_originales | NVARCHAR(1000) | Fila tal como llegó |
| estado | VARCHAR(30) | CHECK: PROCESADO, RECHAZADO, ERROR |
| motivo | VARCHAR(500) | NULL si PROCESADO |
| fecha\_procesamiento | DATETIME2 |  |

Permite investigar cada fila: PROCESADO, RECHAZADO (regla de negocio) o ERROR (falla técnica).

### 6.8. conciliaciones

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_conciliacion | BIGINT | PK |
| id\_ejecucion | BIGINT | FK |
| numero\_cuenta | VARCHAR(30) |  |
| referencia | VARCHAR(100) |  |
| tipo\_movimiento | VARCHAR(30) |  |
| importe\_interno, importe\_externo | DECIMAL(18,2) | NULL si no existe en ese origen |
| resultado | VARCHAR(30) | CHECK (ver sección 8.3) |
| observacion | VARCHAR(300) |  |

### 6.9. auditoria

| Campo | Tipo | Notas |
| --- | --- | --- |
| id\_auditoria | BIGINT | PK |
| id\_usuario | BIGINT | FK; NULL en acciones automáticas |
| entidad | VARCHAR(50) | CLIENTE, CUENTA, USUARIO, PROCESO, EJECUCION |
| id\_registro | BIGINT |  |
| accion | VARCHAR(30) | CHECK: CREACION, MODIFICACION, BAJA\_LOGICA, EJECUCION\_PROCESO |
| datos\_anteriores, datos\_nuevos | JSONB | JSON cuando corresponda |
| fecha\_evento | DATETIME2 |  |

Auditoría append-only: se consulta e inserta, no se modifica ni elimina. Se auditan altas, modificaciones y bajas de clientes, cuentas y usuarios, y el inicio de ejecuciones.

### 6.10. Relaciones

- Cliente 1:N Cuenta; Cuenta 1:N Movimiento.
- Proceso 1:N EjecucionProceso; Usuario 1:N EjecucionProceso.
- EjecucionProceso 1:N Movimiento, RegistroProcesado y Conciliacion.
- Usuario N:M Rol; Usuario 1:N Auditoria.
- `ejecuciones_proceso.id_job_execution` referencia lógicamente a `BATCH_JOB_EXECUTION`.

## 7. Integridad, idempotencia y concurrencia

### 7.1. Integridad

- Claves primarias y foráneas, UNIQUE, CHECK e índices para consultas frecuentes.
- Actualización de saldo y creación del movimiento en una misma transacción: si una falla, ambas se revierten.
- Las restricciones de base de datos son la segunda barrera, además de las validaciones en Spring Boot.

### 7.2. Idempotencia

Dos controles complementarios:

1. **Por archivo:** antes de iniciar, se calcula el SHA-256. En acreditación y débitos, si existe una ejecución del mismo proceso y hash en COMPLETADA o COMPLETADA\_CON\_OBSERVACIONES, se rechaza el inicio. Las ejecuciones FALLIDAS permiten reintento conservando la trazabilidad de cada intento. Las filas ya aplicadas en el intento anterior no se reaplican: la restricción las detecta y se registran como RECHAZADO con el motivo OPERACION\_DUPLICADA, por lo que el reintento puede terminar en COMPLETADA\_CON\_OBSERVACIONES. No hay UNIQUE simple sobre `(id_proceso, hash_archivo)`. Si hay una ejecución EN\_CURSO con el mismo hash, el mensaje debe distinguir ese motivo.
2. **Por operación:** el `UNIQUE (id_cuenta, referencia, tipo_movimiento)` impide aplicar dos veces una misma operación, incluso ante reintentos.

La conciliación no modifica saldos, por lo que permite reejecutar el mismo archivo (se calcula y guarda el hash igualmente).

### 7.3. Concurrencia

- Bloqueo optimista con `@Version` en `Cuenta`. Sin bloqueos pesimistas.
- Ante conflicto de versión se reintenta hasta 3 veces. Si persiste, la fila queda como ERROR y el procesamiento continúa.
- Una sola ejecución EN\_CURSO por proceso, garantizada por un índice único parcial:

```sql
-- Índice único parcial (PostgreSQL)
CREATE UNIQUE INDEX UX_ejecuciones_proceso_en_curso
ON ejecuciones_proceso (id_proceso)
WHERE estado = 'EN_CURSO';
```

- La fila EN\_CURSO se inserta y confirma en su propia transacción antes de lanzar el Job. Si la base rechaza el insert por el índice, el backend responde **409 Conflict** con un mensaje controlado.

### 7.4. Recuperación de ejecuciones interrumpidas

Al arrancar la aplicación se revisan las ejecuciones EN\_CURSO consultando las tablas de metadatos de Spring Batch. Solo si el Job técnico no está activo se marca la ejecución como FALLIDA, con el motivo en `mensaje_error`. No se marcan indiscriminadamente. El diseño asume una única instancia de la aplicación.

### 7.5. Permisos de base de datos

Se usan dos roles de PostgreSQL:

- **Aplicación:** SELECT/INSERT/UPDATE según corresponda (incluidas las tablas BATCH\_\* de Spring Batch), y solo SELECT e INSERT sobre `auditoria`.
- **Migraciones (Flyway):** permisos de DDL.

Se configuran con credenciales distintas (`spring.datasource.*` y `spring.flyway.user/password`), tomadas de variables de entorno, también en CI.

## 8. Procesos batch: reglas de negocio

### 8.1. Formato de archivos CSV

- Codificación UTF-8, separador `;`, primera fila con encabezados, importes con punto decimal (`1234.56`).
- Tamaño máximo 5 MB. El archivo recibido se guarda en un directorio configurable por variable de entorno, fuera del repositorio, con nombre basado en el id de ejecución, y el Job lo lee desde ahí. Se rechaza el archivo completo si está vacío, no es CSV o no tiene los encabezados esperados.

| Proceso | Columnas |
| --- | --- |
| Acreditación | `numero_cuenta;referencia;importe;descripcion` |
| Débitos | `numero_cuenta;referencia;importe;descripcion` |
| Conciliación (externo) | `numero_cuenta;referencia;tipo_movimiento;importe` |

### 8.2. Acreditación y débitos

Flujo: el operador elige el proceso y carga el archivo; el backend valida estructura y hash; crea la ejecución; Spring Batch procesa cada fila y registra su resultado; se finaliza la ejecución.

Validaciones por fila (rechazo funcional con motivo, sin detener el proceso):

- Campos obligatorios y formato válidos.
- Importe mayor que cero.
- Cuenta existente y ACTIVA.
- Referencia no procesada antes para esa cuenta y tipo; una referencia repetida dentro del mismo archivo se rechaza en su segunda aparición.
- Solo débitos: saldo suficiente.

Efecto de una fila válida: actualizar saldo y crear el movimiento (CREDITO o DEBITO) en una misma transacción. Una fila rechazada no modifica saldos ni genera movimientos.

### 8.3. Conciliación bancaria

- Entradas: archivo externo y un período (`periodo_desde`, `periodo_hasta`, inclusivos) que acota los movimientos internos a comparar. Sin período, los movimientos anteriores al archivo aparecerían como faltantes.
- Clave de comparación: `(numero_cuenta, referencia, tipo_movimiento)`.
- No modifica saldos ni genera movimientos.

| Resultado | Descripción |
| --- | --- |
| COINCIDE | Existe en ambos orígenes con el mismo importe |
| DIFERENCIA | Existe en ambos orígenes con importe distinto |
| FALTANTE\_INTERNO | Está en el archivo externo, no en el sistema |
| FALTANTE\_EXTERNO | Está en el sistema (dentro del período), no en el archivo |
| DUPLICADO\_INTERNO | Clave repetida en movimientos internos |
| DUPLICADO\_EXTERNO | Clave repetida en el archivo externo |

La primera aparición de una clave externa se compara normalmente; las repeticiones se registran como DUPLICADO\_EXTERNO. DUPLICADO\_INTERNO es un control defensivo, ya que la restricción UNIQUE lo impide en condiciones normales; sus pruebas requieren una base de prueba con la restricción deshabilitada.

## 9. Spring Batch

### 9.1. Diseño

Cada proceso es un Job con uno o más Steps (lector, procesador, escritor). El procesador valida y devuelve el resultado de cada fila; el escritor aplica saldo y movimiento y persiste `registros_procesados`, de modo que también las filas rechazadas dejan evidencia. El tamaño de chunk es configurable. Parámetros del Job: id de ejecución y ruta del archivo; el id de ejecución hace única cada JobInstance, incluso al reintentar el mismo archivo. La ejecución es asíncrona: la API responde 202 Accepted con el id de ejecución, el Job corre en un TaskExecutor y el frontend consulta el estado periódicamente.

### 9.2. Errores y reintentos

- Rechazos funcionales: no detienen el Job.
- Fallos técnicos transitorios (conflicto de versión, timeout): hasta 3 reintentos.
- Si persisten, la fila queda en ERROR y el Job continúa hasta un umbral configurable de errores. Superado el umbral, el Job falla.
- Un error técnico no recuperable finaliza el Job como fallido.

### 9.3. Mapeo de estados

El estado funcional se deriva del resultado técnico, sin asumir que ambos modelos son equivalentes:

| Resultado técnico | Estado funcional |
| --- | --- |
| COMPLETED sin rechazos ni errores | COMPLETADA |
| COMPLETED con rechazos o errores por fila | COMPLETADA\_CON\_OBSERVACIONES |
| FAILED, STOPPED u otro no exitoso | FALLIDA |

### 9.4. Metadatos y Flyway

- Las tablas técnicas se crean con una migración Flyway que contiene el script oficial de PostgreSQL correspondiente a la versión exacta de Spring Batch elegida.
- Flyway es el único mecanismo que crea y versiona el esquema:

```yaml
spring:
  batch:
    jdbc:
      initialize-schema: never
```

- `ejecuciones_proceso` (historial funcional) y las tablas de Spring Batch (historial técnico) se vinculan por `id_job_execution`.

## 10. API REST

Grupos: autenticación, usuarios, clientes, cuentas, movimientos, procesos, ejecuciones, registros procesados, conciliaciones, dashboard y auditoría.

- GET para consultas; POST para altas y ejecución de procesos; PUT/PATCH para modificaciones; DELETE solo para eliminaciones físicas expresamente permitidas (ninguna en clientes ni cuentas).
- Códigos HTTP adecuados y estructura de error consistente.
- Documentación con Swagger/OpenAPI.

## 11. Frontend

Angular con componentes separados, servicios y modelos tipados; Bootstrap 5.3 y Bootstrap Icons, responsive. No aplica reglas de negocio críticas y no simula resultados que no provengan del backend. El token JWT se guarda en sessionStorage y se envía mediante un interceptor HTTP; un guard protege las rutas según el rol.

| Sección | Contenido |
| --- | --- |
| Login | Formulario, validaciones, sesión, cierre de sesión, redirección al expirar el token |
| Dashboard | Clientes, cuentas activas, movimientos, procesos disponibles, ejecuciones del día, EN\_CURSO, COMPLETADAS, COMPLETADAS\_CON\_OBSERVACIONES, FALLIDAS, últimas ejecuciones |
| Clientes | Listado, búsqueda, alta, edición, detalle, baja lógica |
| Cuentas | Listado, cliente asociado, saldo, estado, alta, edición, baja lógica, movimientos |
| Movimientos | Consulta con filtros |
| Procesos | Listado, descripción, estado, selección de archivo, período (conciliación), botón de ejecución |
| Ejecuciones | Historial: fecha, proceso, usuario, estado, duración, contadores |
| Detalle de ejecución | Registros procesados, rechazados y con error, motivos, movimientos generados, resultados de conciliación, id de ejecución técnica |
| Usuarios | Alta, edición, roles y estado (solo ADMINISTRADOR) |
| Auditoría | Eventos, usuario, entidad, acción, fecha, datos anteriores y nuevos (solo ADMINISTRADOR) |

## 12. Manejo de errores

Manejo centralizado de excepciones (`ManejadorExcepciones`) para: validación, recursos inexistentes, conflictos de negocio, autenticación, autorización, concurrencia, archivos inválidos, referencias duplicadas, procesamiento batch y persistencia. Mensajes comprensibles al usuario; detalle técnico solo en logs internos.

## 13. Testing y calidad

Las pruebas unitarias se escriben junto con cada etapa; las de API y E2E se consolidan al final.

- **Unitarias (JUnit 5, Mockito):** validaciones de cuentas e importes, reglas de acreditación y débito, cálculo de saldos, duplicados, estados de ejecución, mapeo de estados, autorización, resultados de conciliación, auditoría.
- **Integración:** servicios y repositorios, transacciones, migraciones Flyway, metadatos de Spring Batch, vínculo `id_job_execution`, restricciones de unicidad, índice filtrado, concurrencia, autenticación y autorización. Con base de datos de prueba aislada.
- **API (Postman):** métodos, status, estructura JSON, validaciones, autenticación y roles, reglas de negocio, duplicados, 409 por ejecución simultánea, auditoría. Se verifica además la base de datos, no solo el status HTTP.
- **E2E (Playwright):** login correcto e incorrecto, acceso según rol, clientes, cuentas, carga y ejecución de procesos, resultados y rechazos, movimientos generados, ausencia de acreditaciones duplicadas, historial, conciliación, auditoría, rechazo de ejecuciones simultáneas.

**Datos de prueba** (ficticios), con resultados esperados definidos de antemano: filas válidas, campos vacíos, cuentas inexistentes o inactivas, importes inválidos, saldo insuficiente, referencias duplicadas, diferencias de importe, archivos vacíos o con estructura incorrecta, y CSV con separador o codificación erróneos.

## 14. Git y GitHub Actions

Repositorio `bankbatch-simulator`. Ramas: `main` (estable), `develop` (integración), `feature/*`, `fix/*`, `release/*`. Cada cambio entra por Pull Request.

| Flujo | Pasos |
| --- | --- |
| Backend | Java, dependencias Maven, compilación, pruebas unitarias y de integración, reporte |
| Frontend | Node.js, dependencias, compilación Angular, pruebas disponibles |
| E2E | Playwright en flujo independiente sobre un entorno de prueba |

No se integran cambios que rompan las validaciones obligatorias.

## 15. Estructura del repositorio

```text
bankbatch-simulator/
+-- backend/            (src, pom.xml, README.md)
+-- frontend/           (src, package.json, README.md)
+-- pruebas/
|   +-- postman/
|   +-- e2e/            (Playwright)
+-- archivos/
|   +-- acreditacion/
|   +-- debitos/
|   +-- conciliacion/   (CSV ficticios de ejemplo y de prueba)
+-- base-datos/         (datos iniciales ficticios y diagramas)
+-- documentacion/      (arquitectura, modelo-datos, procesos, testing)
+-- .github/workflows/  (backend.yml, frontend.yml, pruebas-e2e.yml)
+-- .gitignore
+-- README.md
```

Las migraciones viven en `backend/src/main/resources/db/migration/` y son la fuente de verdad del esquema (`V1__...`, `V2__...`). No se suben contraseñas, claves ni credenciales. Los archivos de entrada son ficticios.

## 16. Plan de desarrollo

| Etapa | Desarrollo | Resultado |
| --- | --- | --- |
| 1 | Repositorio, Git y GitHub | Repositorio inicial |
| 2 | Java 21, Maven y Spring Boot | Backend funcionando |
| 3 | PostgreSQL y Flyway | Conexión y migraciones operativas |
| 4 | Modelo de datos, migraciones (incluye metadatos de Spring Batch) y datos iniciales | Esquema versionado |
| 5 | Spring Security, usuarios, roles y JWT | Login y autorización |
| 6 | Clientes y cuentas, bajas lógicas, auditoría | Backend bancario básico |
| 7 | Servicios, transacciones y concurrencia | Reglas de negocio e integridad |
| 8 | Angular: login, dashboard, clientes y cuentas | Panel base conectado |
| 9 | Proceso de acreditación | Primer Job funcional |
| 10 | Proceso de débitos | Segundo Job funcional |
| 11 | Proceso de conciliación | Tercer Job funcional |
| 12 | Idempotencia, recuperación de ejecuciones y reportes | Trazabilidad completa |
| 13 | Angular: procesos, ejecuciones, detalle, conciliaciones, usuarios y auditoría | Aplicación completa |
| 14 | Consolidación de pruebas unitarias e integración | Cobertura backend |
| 15 | Postman | Testing de API |
| 16 | Playwright | Testing E2E |
| 17 | GitHub Actions | Integración continua |
| 18 | Documentación y README | Portfolio terminado |

## 17. Criterios de aceptación

El proyecto se considera terminado cuando:

1. Se puede ingresar con credenciales válidas y el acceso respeta los roles.
2. Se administran clientes, cuentas y usuarios, y se consultan los movimientos.
3. Se cargan archivos CSV ficticios y se ejecutan los tres procesos.
4. Los registros válidos se procesan, los incorrectos se rechazan con su motivo y los saldos se actualizan de forma consistente.
5. Ninguna operación se aplica dos veces y las modificaciones concurrentes de cuentas no pisan saldos.
6. No se pueden iniciar dos ejecuciones simultáneas del mismo proceso.
7. Cada ejecución identifica quién la lanzó y se relaciona con su registro técnico de Spring Batch.
8. Los resultados de conciliación (faltantes, diferencias y duplicados) y la auditoría son consultables.
9. La API está documentada y las pruebas principales están automatizadas.
10. Compila en GitHub Actions y puede ejecutarse siguiendo el README.

BankBatch Simulator es un proyecto educativo y demostrativo. No maneja dinero real ni se conecta con sistemas bancarios productivos.

---

## Anexo A. Decisiones tomadas en la consolidación

| Tema | Decisión | Origen del conflicto |
| --- | --- | --- |
| Tipos de movimiento | CREDITO y DEBITO | La v1.3 usaba ACREDITACION |
| Campos de usuarios | Se restauran nombre, apellido, correo, estado (ACTIVO/INACTIVO/BLOQUEADO) y ultimo\_acceso; la columna es contrasena\_hash | La v1.3 los había reducido |
| Contadores de ejecución | total\_leidos, total\_exitosos, total\_rechazados, total\_errores | La v1.3 los reducía y mezclaba errores |
| Estado de fila | Se restaura ERROR | Faltaba en la v1.3 |
| Saldos de movimiento | saldo\_anterior y saldo\_posterior | La v1.3 solo tenía saldo\_resultante |
| Campo origen de movimientos | Se elimina: el origen es la ejecución | Redundante |
| Período de conciliación | periodo\_desde y periodo\_hasta obligatorios | No estaba definido |
| Saldo negativo | No permitido (CHECK saldo >= 0) | No estaba definido |
| Acciones de auditoría | CREACION, MODIFICACION, BAJA\_LOGICA, EJECUCION\_PROCESO | Había ELIMINACION sin uso |
| Permisos de OPERADOR | Solo consulta y ejecución de procesos | Ambiguo |
| Formato CSV | UTF-8, separador `;`, máximo 5 MB | No estaba definido |
| Hash en conciliación | Se guarda, pero no bloquea reejecuciones | No estaba definido |
| Reintentos | 3 reintentos y umbral configurable de errores por fila | No estaba definido |
| Orden del plan | Angular base antes de los Jobs; tests unitarios junto con cada etapa | Diferían v1.2 y v1.3 |
| Carpeta de pruebas E2E | `pruebas/e2e` | Diferían v1.2 y v1.3 |

### Correcciones de la revisión final

- Ejecución asíncrona: 202 Accepted y consulta periódica del estado.
- Parámetros del Job definidos para que cada intento sea una JobInstance distinta.
- Reintento tras una ejecución FALLIDA: las filas ya aplicadas se registran como RECHAZADO (OPERACION\_DUPLICADA).
- Almacenamiento del archivo recibido definido; los archivos inválidos o ya procesados no crean ejecución.
- Reglas de baja lógica: cuenta con saldo cero y cliente sin cuentas abiertas.
- Zona horaria del período de conciliación: America/Argentina/Buenos\_Aires.
- El estado del usuario se valida en cada solicitud.
- Permisos sobre las tablas BATCH\_\* y excepción de la clave compuesta de usuarios\_roles.
- Alcance de los reportes de procesamiento definido.
- Almacenamiento del token en el frontend: sessionStorage.

## Anexo B. Cambio de motor en la versión 1.5

El motor de base de datos pasa de SQL Server a **PostgreSQL 18** (instalación local). Motivo: en el equipo de desarrollo SQL Server no arrancaba por el tamaño de sector del disco NVMe, y PostgreSQL mantiene todas las decisiones de diseño con una instalación más simple. Las tablas de la sección 6 conservan su notación original; en PostgreSQL se implementan así:

| Notación del documento | PostgreSQL |
| --- | --- |
| BIGINT IDENTITY | BIGINT GENERATED ALWAYS AS IDENTITY |
| DATETIME2 | TIMESTAMPTZ (se guarda en UTC) |
| BIT | BOOLEAN |
| JSONB (auditoría) | JSONB |
| VARCHAR(n), CHAR(64), INT | Igual |
| DECIMAL(18,2) | NUMERIC(18,2) |
| Índice único filtrado | Índice único parcial con WHERE estado = 'EN\_CURSO' |
| Principales de SQL Server | Roles bankbatch\_migraciones y bankbatch\_app |
| Puerto 1433 y mssql-jdbc | Puerto 5432 y org.postgresql:postgresql |
| Script de metadatos de Spring Batch | Script oficial de PostgreSQL de la versión elegida, como migración Flyway |

La auditoría append-only se logra quitando UPDATE y DELETE sobre auditoria al rol de la aplicación, en una migración.
