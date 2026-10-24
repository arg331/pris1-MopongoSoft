# SAD-UAL — Sistema de Reservas de Áreas Deportivas de la UAL

Proyecto desarrollado para la asignatura **PRIS1 – Procesos de Ingeniería del Software 1**
(Grado en Ingeniería Informática, Universidad de Almería, curso 2026/2027).

**Grupo 07 · MopongoSoft S.L. · Variación 7** — Eventos masivos, escuelas infantil-juveniles
y reservas institucionales.

## Integrantes

| Integrante | Rol principal | Respaldo |
|---|---|---|
| Abel Rodríguez Gutiérrez | Scrum Master | María Camila Soto Zapata |
| Daniel Almendros Arévalo | Product Owner · Responsable Frontend | PO: Javier Zúñiga · Front: Abel Rodríguez |
| María Camila Soto Zapata | Responsable Backend | Javier Zúñiga Carmona |
| Javier Zúñiga Carmona | Responsable Base de Datos | María Camila Soto Zapata |

## Objetivo de la Release V1 (Hito 1 · 26/10/2026)

Que un usuario de la UAL pueda iniciar sesión, consultar la disponibilidad de una instalación
y reservarla sin riesgo de doble reserva, y consultar y cancelar sus reservas.

### Estado de la Release V1

Priorización completa y criterios de aceptación en [`docs/funcionalidades-v1.md`](docs/funcionalidades-v1.md).

Reglas de reserva aplicadas (detalle y estado en [`docs/requisitos-cliente.md`](docs/requisitos-cliente.md)):
una reserva por día (P1), al menos 3 horas de antelación (P2), sin reservar el resto de la semana tras no
presentarse (P3), reservas individuales solo para la comunidad universitaria (P4), los bloqueos del
administrador tienen prioridad (P7) y cero dobles reservas (P10). **Pendientes de validar por el cliente.**

| ID | Funcionalidad | MoSCoW | Estado |
|---|---|---|---|
| F01 | Inicio de sesión con perfiles | Must | ✅ Implementada (backend + pantalla) |
| F02 | Catálogo de instalaciones | Must | ✅ Implementada (backend + pantalla) |
| F03 | Consulta de disponibilidad | Must | ✅ Implementada (backend + pantalla) |
| F04 | Reserva de una franja (cero dobles reservas) | Must | ✅ Implementada (backend + pantalla) |
| F05 | Mis reservas y cancelación | Must | ✅ Implementada (backend + pantalla) |
| F06 | Panel admin de instalaciones | Should | Pendiente |
| F07 | Gestión básica de usuarios | Should | Pendiente |
| F08 | Bloqueo de franjas | Should | Modelo de datos listo; bloqueos visibles en F03 |
| F09 | Límites por perfil | Could | Columnas preparadas en `rol`; el límite común (P1) ya se aplica |
| F10 | Consulta general de reservas | Could | Vista `v_reservas_detalle` lista |
| F11 | Experiencia móvil optimizada | Could | Pantallas F01–F05 adaptadas a móvil |
| F12 | Sanción por no presentarse (P3) | Could | ✅ BD + endpoint de administración |
| — | Variación 7 (eventos, escuelas, reservas institucionales) | Won't (V1) | Incremento 2 · tablas ya creadas |

## Tecnologías

| Capa | Tecnología |
|---|---|
| Frontend | Angular 21, TypeScript, Angular Material, RxJS · pruebas con Vitest |
| Backend | Java 21 + Spring Boot 3.5 (Web, Data JPA, Security, OAuth2 Resource Server/JWT, Validation) |
| Base de datos | PostgreSQL 16 (restricción `EXCLUDE` con `btree_gist`, triggers, funciones) |
| Pruebas backend | JUnit 5, Mockito, Spring MockMvc, Testcontainers (PostgreSQL real) |
| Otras herramientas | Git/GitHub (Issues, Projects, pull requests), Docker Compose, Postman, Graphviz |

## Estructura del repositorio

```
grupo-07/                      (organización GitHub pris1)
├── README.md
├── docker-compose.yml         → PostgreSQL de desarrollo con esquema y datos demo
├── frontend/                  → App Angular (ver frontend/README.md)
├── backend/                   → API REST Spring Boot (ver backend/README.md)
├── database/
│   ├── 01_schema.sql          → Esquema completo (14 tablas, triggers, funciones y vistas)
│   ├── 02_seed.sql            → Datos de demostración para el Hito 1
│   └── 03_pruebas.sql         → Pruebas del modelo de datos (T1–T13)
└── docs/
    ├── funcionalidades-v1.md  → Priorización MoSCoW y criterios de aceptación
    ├── requisitos-cliente.md  → Preguntas P1–P10 y decisiones (pendientes de validar)
    └── modelo-datos/
        ├── MER.png            → Modelo Entidad-Relación
        └── ModeloRelacional.png → Modelo relacional (PostgreSQL)
```

## Puesta en marcha (entorno de desarrollo)

Requisitos: Docker Desktop **arrancado**, Java 21 (el mismo para todo el equipo), Maven 3.9+ (o IntelliJ), Node.js 20.19+ / 22.12+.

```bash
# 1. Base de datos (crea el esquema y carga los datos demo) → localhost:5433
docker compose up -d
#    Sin Docker: createdb sadual && psql -d sadual -f database/01_schema.sql -f database/02_seed.sql
#    y arrancar el backend con SADUAL_DB_URL=jdbc:postgresql://localhost:5432/sadual (y su usuario/contraseña)

# 2. Backend → http://localhost:8080
cd backend
mvn spring-boot:run

# 3. Frontend → http://localhost:4200 (redirige /api al backend)
cd frontend
npm install
npm start
```

> **¿Tienes PostgreSQL instalado en tu equipo?** Por eso la base de datos de Docker usa el puerto **5433**
> y no el 5432: si ambos usaran el 5432, el backend se conectaría a tu PostgreSQL local y fallaría la
> contraseña. Si el 5433 también está ocupado: `SADUAL_DB_PORT=5434 docker compose up -d` y arrancar el
> backend con `SADUAL_DB_URL=jdbc:postgresql://localhost:5434/sadual`.
>
> En Windows: `$env:SADUAL_DB_URL="jdbc:postgresql://localhost:5434/sadual"; mvn spring-boot:run` (PowerShell).

Usuarios de demostración (solo desarrollo), todos con la contraseña `Sadual2026!`:
`alumna.demo@ual.es`, `pdi.demo@ual.es`, `pas.demo@ual.es`, `externo.demo@correo.test`,
`admin.deportes@ual.es`. En la pantalla de login hay atajos para rellenarlos. El usuario externo puede
entrar y consultar, pero no reservar (P4).

## Pruebas

| Qué | Comando | Necesita |
|---|---|---|
| Modelo de datos (T1–T13) | `psql -d sadual -f database/03_pruebas.sql` | BD creada |
| Backend · unitarias | `cd backend && mvn test` | — |
| Backend · integración (incluye pruebas de reservas simultáneas) | `cd backend && mvn verify` | Docker |
| Frontend | `cd frontend && npm test` | — |

## Flujo de trabajo

- `main` protegida: solo recibe cambios por pull request revisado por otro integrante.
- Una rama por funcionalidad: `feature/F04-crear-reserva`, `fix/...`, `docs/...`.
- Commits con prefijo: `feat:`, `fix:`, `docs:`, `db:`, `test:`.
- Cada funcionalidad de la Release V1 es un Issue en GitHub Projects, con su criterio de aceptación.

## Documentación

El modelo de datos (MER y modelo relacional) está en `docs/modelo-datos/`. El resto de la
documentación del curso (G1–G4) se irá añadiendo a `docs/`.
