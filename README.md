# SAD-UAL — Sistema de Reservas de Áreas Deportivas de la UAL

Proyecto desarrollado para la asignatura **PRIS1 – Procesos de Ingeniería del Software 1**
(Grado en Ingeniería Informática, Universidad de Almería, curso 2026/2027).

**Grupo 07 · MopongoSoft S.L. · Variación 7** — Eventos masivos, escuelas infantil-juveniles
y reservas institucionales.

## Integrantes

| Integrante | Rol principal | Respaldo |
|---|---|---|
| Abel Rodríguez Gutiérrez | Scrum Master | María Camila Soto |
| Daniel Almendros Arévalo | Product Owner · Responsable Frontend | PO: Javier Zúñiga · Front: Abel Rodríguez |
| María Camila Soto Zapata | Responsable Backend | Javier Zúñiga |
| Javier Zúñiga Carmona | Responsable Base de Datos | María Camila Soto |

## Objetivo de la Release V1

Que un usuario de la UAL pueda iniciar sesión, consultar la disponibilidad de una instalación
y reservarla sin riesgo de doble reserva, con un panel básico de administración para gestionar
instalaciones, usuarios y bloqueos. Hito 1: **26/10/2026**.

### Funcionalidades previstas para la Release V1

> Propuesta de priorización MoSCoW, pendiente de validación por el Product Owner.

| Prioridad | Funcionalidad |
|---|---|
| **Must** | Inicio de sesión con perfiles (alumnado, PDI, PAS, externo, administración) |
| **Must** | Consulta del catálogo de instalaciones |
| **Must** | Consulta de disponibilidad por franjas |
| **Must** | Crear y cancelar reservas sin dobles reservas |
| **Should** | Panel de administración: alta y baja de instalaciones |
| **Should** | Gestión básica de usuarios |
| **Should** | Bloqueo de instalaciones (docencia, mantenimiento) |
| **Could** | Eventos masivos, escuelas infantil-juveniles y reservas institucionales (Variación 7, Incremento 2) |

## Tecnologías

| Capa | Tecnología |
|---|---|
| Frontend | Angular, TypeScript, Angular Material, RxJS |
| Backend | Java 21 + Spring Boot (Spring Web, Spring Data JPA, Spring Security, JWT) |
| Base de datos | PostgreSQL 16 (el script requiere 15 o superior) |
| Otras herramientas | Git/GitHub (Issues, Projects, pull requests), Docker Compose, Postman, Graphviz |

## Estructura del repositorio

```
pris1-MopongoSoft/
├── README.md
├── .gitignore
├── frontend/          → App Angular (ver frontend/README.md)
├── backend/           → API Spring Boot (ver backend/README.md)
├── database/
│   ├── 01_schema.sql  → Esquema completo (14 tablas, triggers, funciones y vistas)
│   └── 02_seed.sql    → Datos de demostración para el Hito 1
└── docs/
    └── modelo-datos/
        ├── MER.png                → Modelo Entidad-Relación
        └── ModeloRelacional.pdf   → Modelo relacional (PostgreSQL)
```

## Puesta en marcha (entorno de desarrollo)

### 1. Base de datos

```bash
createdb sadual
psql -d sadual -f database/01_schema.sql
psql -d sadual -f database/02_seed.sql
```

### 2. Backend (Spring Boot)

> Los proyectos de backend y frontend se generan esta semana; los comandos son los previstos.

```bash
cd backend
# configurar application.properties con la conexión a PostgreSQL
./mvnw spring-boot:run
```

### 3. Frontend (Angular)

```bash
cd frontend
npm install
ng serve
```

Credenciales de demostración (entorno de desarrollo): cualquier email `*.demo@ual.es` /
`*.demo@correo.test` con contraseña `Sadual2026!`.

## Flujo de trabajo

- `main` protegida: solo recibe cambios por pull request revisado por otro integrante.
- Una rama por funcionalidad: `feature/F04-crear-reserva`, `fix/...`, `docs/...`.
- Commits con prefijo: `feat:`, `fix:`, `docs:`, `db:`.
- Cada funcionalidad de la Release V1 es un Issue en GitHub Projects, con su criterio de aceptación.

## Documentación

El modelo de datos (MER y modelo relacional) está en `docs/modelo-datos/`. El resto de la
documentación del curso (G1–G4) se irá añadiendo a `docs/`.
