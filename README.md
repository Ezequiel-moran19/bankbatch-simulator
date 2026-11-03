# BankBatch Simulator

Aplicación web Full Stack que simula procesos batch bancarios sobre datos ficticios: acreditación de haberes, débitos automáticos y conciliación bancaria. Proyecto educativo y de portfolio; no maneja dinero real ni se conecta con sistemas bancarios.

> Estado: **Etapa 1 completada** (repositorio inicial). El resto del proyecto se construye de forma incremental según el plan de desarrollo.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | Angular, TypeScript, Bootstrap 5.3, Bootstrap Icons |
| Backend | Java 21, Spring Boot, Spring Batch, Spring Security + JWT, JPA/Hibernate, Maven |
| Base de datos | SQL Server, migraciones con Flyway |
| Testing | JUnit 5, Mockito, Postman, Playwright |
| CI | GitHub Actions |

## Estructura del repositorio

```text
bankbatch-simulator/
├── backend/            API REST y procesos batch (Spring Boot)
├── frontend/           Panel administrativo (Angular)
├── pruebas/
│   ├── postman/        Colecciones de API
│   └── e2e/            Pruebas Playwright
├── archivos/           CSV ficticios de ejemplo y de prueba
│   ├── acreditacion/
│   ├── debitos/
│   └── conciliacion/
├── base-datos/         Datos iniciales ficticios y diagramas
├── documentacion/      Arquitectura, modelo de datos, procesos y testing
└── .github/            Plantilla de PR y workflows de CI
```

Las migraciones de Flyway viven en `backend/src/main/resources/db/migration/` y son la única fuente de verdad del esquema.

## Configuración local

1. Copiar `.env.example` a `.env` y completar los valores (el archivo `.env` no se versiona).
2. Las credenciales, la clave JWT y cualquier secreto se leen siempre desde variables de entorno.

Los requisitos y comandos de cada parte se documentan en `backend/README.md` y `frontend/README.md` a medida que se crean.

## Flujo de trabajo con Git

Ramas:

- `main`: versiones estables.
- `develop`: integración de funcionalidades.
- `feature/<nombre>`: nuevas funcionalidades, creadas desde `develop`.
- `fix/<nombre>`: corrección de errores.
- `release/vX.Y.Z`: preparación de versiones estables.

Reglas:

- Cada cambio entra por Pull Request hacia `develop`; `main` solo recibe `release/*`.
- Los PR deben pasar las validaciones de CI antes de integrarse (cuando estén configuradas).
- Mensajes de commit en español, con prefijo: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.

Ejemplos de ramas: `feature/configuracion-inicial`, `feature/modelo-base-datos`, `feature/autenticacion-usuarios`, `feature/proceso-acreditacion`, `fix/error-acreditacion-duplicada`, `release/v1.0.0`.

## Plan de desarrollo

| Etapa | Desarrollo |
|---|---|
| 1 | Repositorio, Git y GitHub |
| 2 | Java 21, Maven y Spring Boot |
| 3 | SQL Server y Flyway |
| 4 | Modelo de datos, migraciones y datos iniciales |
| 5 | Spring Security, usuarios, roles y JWT |
| 6 | Clientes y cuentas, bajas lógicas, auditoría |
| 7 | Servicios, transacciones y concurrencia |
| 8 | Angular: login, dashboard, clientes y cuentas |
| 9 | Proceso de acreditación |
| 10 | Proceso de débitos |
| 11 | Proceso de conciliación |
| 12 | Idempotencia, recuperación de ejecuciones y reportes |
| 13 | Angular: procesos, ejecuciones, conciliaciones, usuarios y auditoría |
| 14 | Consolidación de pruebas unitarias e integración |
| 15 | Postman |
| 16 | Playwright |
| 17 | GitHub Actions |
| 18 | Documentación final |

## Seguridad

No se suben contraseñas, claves, credenciales ni archivos con información sensible. Todos los datos y archivos de entrada del repositorio son ficticios.
