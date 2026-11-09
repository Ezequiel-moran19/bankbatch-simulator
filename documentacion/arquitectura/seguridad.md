# Seguridad y autenticación

Estado: implementada y validada el 08/11/2026 con pruebas automatizadas y con Postman.

## Flujo

1. El cliente envía usuario y contraseña a `POST /api/autenticacion/inicio-sesion`.
2. `ServicioAutenticacion` normaliza el usuario a minúsculas, verifica la contraseña con BCrypt y exige que el usuario esté ACTIVO. Si todo es correcto, registra `ultimo_acceso`.
3. `ServicioJwt` emite un token HS256 de 15 minutos con los reclamos `sub`, `iat`, `exp` y `roles` (informativo).
4. En cada solicitud posterior se valida la firma y el vencimiento sin tolerancia de reloj. Después, `ConvertidorAutenticacionJwt` consulta el usuario en la base: debe existir y estar ACTIVO, y sus permisos (`ROLE_<nombre>`) salen de los roles actuales de la base, no del token.

Un bloqueo o un cambio de rol rige en la siguiente solicitud, sin esperar a que venza el token.

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/autenticacion/inicio-sesion` | Público |
| GET | `/api/autenticacion/perfil` | Autenticado |
| GET | `/actuator/health` | Público |

El resto de las rutas exige un token válido.

## Decisiones

- JWT HS256, sin refresh token. El secreto viene de `BANKBATCH_JWT_SECRETO` (mínimo 32 caracteres) y nunca se versiona. La duración es de 15 minutos.
- Contraseñas con BCrypt. Las que superan los 72 bytes en UTF-8 se rechazan en el login y en el administrador inicial.
- Usuario inexistente, contraseña incorrecta, usuario inactivo y usuario bloqueado devuelven la misma respuesta 401. Para usuarios inexistentes se compara contra un hash ficticio, así el tiempo de respuesta no revela si la cuenta existe.
- El nombre de usuario y el correo se guardan en minúsculas, garantizado por la entidad y por la migración V10.
- Administrador inicial: solo en el perfil `dev` y solo si la tabla `usuarios` está vacía. Variables: `BANKBATCH_ADMIN_CLAVE_INICIAL` (obligatoria), `BANKBATCH_ADMIN_NOMBRE_USUARIO` (por defecto `admin`) y `BANKBATCH_ADMIN_CORREO` (por defecto `admin@bankbatch.local`).
- API sin sesiones y sin CSRF (la autenticación va en el encabezado, no en cookies).
- CORS limitado a `http://localhost:4200`; se cambia con `bankbatch.seguridad.cors.origenes-permitidos`.
- Los errores 400, 401 y 403 tienen una estructura común: `fecha`, `estado`, `error`, `mensaje`, `ruta` y `detalles`.

## Pruebas

- Automatizadas (JUnit 5, Mockito y MockMvc): servicio de JWT, servicio de autenticación, validador de contraseñas, entidad, restricciones de la base, inicializador, configuración de seguridad, controlador y errores HTTP.
- Postman (colección `BankBatch - API`, carpetas `00 - Verificacion` y `01 - Autenticacion`): estado del backend, inicio de sesión, perfil, perfil sin token, contraseña incorrecta y cuerpo vacío.

## Pendiente para etapas siguientes

- Permisos por rol en cada recurso (`@PreAuthorize`), según la matriz del documento v1.5.
- Gestión de usuarios (alta, modificación y cambio de estado) solo para ADMINISTRADOR.
- El bloqueo automático por intentos fallidos queda fuera de la versión 1.0.
