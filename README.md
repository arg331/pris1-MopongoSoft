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

## Tecnologías

| Capa | Tecnología |
|---|---|
| Frontend | React + Vite, React Router, Tailwind CSS, Axios |
| Backend | PHP 8.3+ con Laravel (API REST JSON), Laravel Sanctum |
| Base de datos | PostgreSQL 16 |
| Otras herramientas | Git/GitHub (Issues, Projects, pull requests), Docker Compose, Postman, Graphviz |

## Estructura del repositorio

```
grupo07-sadual/
├── README.md
├── .gitignore
├── frontend/          → App Angular (ver frontend/README.md)
├── backend/           → API Spring Boot (ver backend/README.md)
├── database/
│   ├── 01_schema.sql  → Esquema completo (14 tablas, triggers, funciones y vistas)
│   ├── 02_seed.sql    → Datos de demostración para el Hito 1
│   └── 03_pruebas.sql → Batería de verificación de las reglas críticas
└── docs/
    ├── funcionalidades-v1.md   → Priorización MoSCoW
    ├── G1/ G2/ G3/ G4/         → Entregas consolidadas del curso
    └── modelo-datos/
        ├── MER.dot / MER.png
        └── ModeloRelacional.dot / ModeloRelacional.pdf
```

## Puesta en marcha (entorno de desarrollo)

### 1. Base de datos

```bash
createdb sadual
psql -d sadual -f database/01_schema.sql
psql -d sadual -f database/02_seed.sql
psql -d sadual -f database/03_pruebas.sql   # opcional, verificación
```

### 2. Backend (Laravel)

```bash
cd backend
composer install
cp .env.example .env    # configurar conexión a PostgreSQL
php artisan serve
```

### 3. Frontend (React)

```bash
cd frontend
npm install
npm run dev
```

Credenciales de demostración (entorno de desarrollo): cualquier email `*.demo@ual.es` /
`*.demo@correo.test` con contraseña `Sadual2026!`.

## Flujo de trabajo

- `main` protegida: solo recibe cambios por pull request revisado por otro integrante.
- Una rama por funcionalidad: `feature/F04-crear-reserva`, `fix/...`, `docs/...`.
- Commits con prefijo: `feat:`, `fix:`, `docs:`, `db:`.
- Cada funcionalidad (F01–F11) es un Issue en GitHub Projects, con su criterio de aceptación.

## Documentación

La documentación completa de análisis y diseño (G1–G4) está en `docs/`, incluyendo el informe
oficial de esta entrega: `docs/G4/G4_Grupo07_PreparacionTecnica.pdf`.
