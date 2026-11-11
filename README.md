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

**Reglas de negocio validadas por el cliente el 25/10/2026** (detalle en [`docs/requisitos-cliente.md`](docs/requisitos-cliente.md),
evidencia en [`docs/cliente/`](docs/cliente/)): una reserva por día (P1), al menos 3 horas de antelación (P2), sin
reservas nuevas hasta el domingo tras no presentarse (P3), reservas individuales solo para la comunidad
universitaria (P4/P17), los bloqueos de la administración tienen prioridad (P7), cero dobles reservas (P10),
horario L–V 8–22 y sábados 9–14 con festivos como cierres (P11), franjas de 90 min en pádel y 60 en el resto
(P12) y reserva de la unidad completa (P13).

| ID | Funcionalidad | MoSCoW | Estado |
|---|---|---|---|
| F01 | Inicio de sesión con perfiles | Must | ✅ Implementada (backend + pantalla) |
| F02 | Catálogo de instalaciones | Must | ✅ Implementada (backend + pantalla) |
| F03 | Consulta de disponibilidad | Must | ✅ Implementada (backend + pantalla) |
| F04 | Reserva de una franja (cero dobles reservas) | Must | ✅ Implementada (backend + pantalla) |
| F05 | Mis reservas y cancelación | Must | ✅ Implementada (backend + pantalla) |
| F06 | Panel admin de instalaciones (duración de franja configurable, P12) | **Must** (núcleo común) | ✅ Incremento 2 (backend + pantalla) |
| F07 | Gestión de usuarios (alta de la comunidad UAL, P16) | **Must** (núcleo común) | ✅ Incremento 2 (backend + pantalla) |
| F08 | Bloqueo de franjas y cierres por festivos (P7, P11) | Should | ✅ Incremento 2 (backend + pantalla) |
| F09 | Límites por perfil | Could | Columnas preparadas en `rol`; el límite común (P1) ya se aplica |
| F10 | Consulta general de reservas | **Must** (núcleo común) | ✅ Incremento 2 (backend + pantalla) |
| F11 | Experiencia móvil optimizada | Could | Pantallas adaptadas a móvil |
| F12 | Sanción por no presentarse (P3) | Could | ✅ BD, API y botón en la consulta general |
| V7 | Registro de tutores y representantes (P17) | Incremento 2 | ✅ backend + pantalla |
| V7 | Reservas institucionales con autorización (P21) | Incremento 2 | ✅ backend + pantallas (representante y bandeja de administración) |
| V7 | Eventos masivos con aforo (P18) | Incremento 2 | ✅ backend + pantallas (usuarios y administración) |
| V7 | Escuelas, menores y consentimiento RGPD (P19, P20, P22) | Incremento 2 | ✅ backend + pantallas (tutor y administración) |
| Inc. 3 | Informes de ocupación, uso, cancelaciones y no presentados (P9) | Incremento 3 | ✅ backend + pantalla |

> Estado a 09/11/2026: todo lo marcado como Incremento 2/3 está implementado y probado en la rama
> `feature/variacion7-backend`, **pendiente de pull request y revisión** para entrar en `main`. Las reglas
> de la Variación 7 (P23–P46) las validó el cliente el 11/11/2026 y ya están aplicadas; las marcadas con ❓
> en `docs/requisitos-cliente.md` (P47–P60) esperan su respuesta.

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
│   └── 03_pruebas.sql         → Pruebas del modelo de datos (T1–T16)
└── docs/
    ├── funcionalidades-v1.md  → Priorización MoSCoW, criterios de aceptación y estado
    ├── requisitos-cliente.md  → Preguntas P1–P22 y decisiones validadas por el cliente
    ├── cliente/               → Propuesta enviada (24/10) y respuesta del cliente (25/10)
    └── modelo-datos/
        ├── MER.png / MER.pdf                       → Modelo Entidad-Relación
        ├── ModeloRelacional.png / ModeloRelacional.pdf → Modelo relacional (PostgreSQL)
        └── fuentes/           → Archivos originales (.dot) y script que los genera (Graphviz)
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
`alumna.demo@ual.es`, `pdi.demo@ual.es`, `pas.demo@ual.es`, `externo.demo@correo.test` (representante del
Club Deportivo Demo Almería), `tutor.demo@correo.test` (tutora de Hugo, inscrito en la escuela de natación) y
`admin.deportes@ual.es`. En la pantalla de login hay atajos para rellenarlos. Los usuarios externos no hacen
reservas individuales (P4/P17).

`docker compose` carga además `database/04_demo_hito2.sql`, con reservas de la semana de la defensa (15–18/12).
**Si ya tenías la base de datos creada**, recréala para cargar el esquema v1.2 y los datos nuevos:
`docker compose down -v && docker compose up -d` (borra los datos locales).

## Pruebas

| Qué | Comando | Necesita |
|---|---|---|
| Modelo de datos (T1–T23, incluida la Variación 7) | `psql -d sadual -f database/03_pruebas.sql` | BD creada |
| Backend · unitarias | `cd backend && mvn test` | — |
| Backend · integración (reservas simultáneas, Variación 7, panel de administración e informes) | `cd backend && mvn verify` | Docker |
| Frontend | `cd frontend && npm test` | — |

Resultados a 11/11/2026: modelo de datos 23/23 · backend 46 unitarias y 128 de integración · frontend 76 pruebas
y build de producción sin avisos. Además se recorrieron las pantallas con Chrome (sin errores de JavaScript).

## Guion de la demo del Hito 1

Con los tres servicios arrancados, en `http://localhost:4200`:

1. **Login (F01).** Atajo *Alumnado* → entra Lucía, perfil ALUMNADO.
2. **Catálogo (F02).** Filtrar por *Pádel* → Pistas de pádel 1 y 2.
3. **Disponibilidad (F03).** *Pista de pádel 1*, martes **27/10**: franjas de 90 min (P12), la de 09:30 aparece *Ocupada*.
4. **Reservar (F04).** La alumna ya tiene la de las 09:30, así que elegir otro día, por ejemplo **28/10** a las 11:00 → *Reserva confirmada*.
   Intentar otra el mismo 28/10 en tenis → *"Ya tienes una reserva ese día"* (P1).
5. **Bloqueos y cierres.** *Pabellón cubierto* el **26/10** por la mañana → *Bloqueada · docencia* (P7).
   Cualquier instalación el **8/12** → todo *Cerrado* por festivo (P11).
6. **Mis reservas (F05).** Cancelar la reserva del 28/10 → la franja vuelve a estar libre al instante.
7. **Perfiles (P4/P17).** Cerrar sesión y entrar con *Externo* → puede consultar, pero no reservar.
8. **Cero dobles reservas (P10).** Mostrar `ConcurrenciaReservaIT`: 10 peticiones simultáneas → 1 × 201 y 9 × 409.

> Ojo con la hora: el día de la demo, las franjas que empiezan en menos de 3 horas salen *Fuera de plazo* (P2).

## Guion de la demo del Hito 2 (14/12/2026)

Con la base de datos recreada (incluye `04_demo_hito2.sql`) y los tres servicios arrancados:

1. **Registro (P17, P23, P24).** Desde el login, *Crea tu cuenta* → representante con el CIF `G00000000` →
   queda **pendiente**; si intenta entrar, se le avisa. Admin → *Usuarios* → filtro *Representantes pendientes*
   → *Validar y activar* → ya puede entrar. (Un tutor debe dar teléfono y entra directamente.)
2. **Reserva institucional (P21, P27, P28).** Representante (atajo *Representante*): *Institucional* → fútbol 7,
   un día laborable con al menos 48 h de margen → pulsar la primera y la última franja (hasta 4 h) →
   *Solicitar*: queda **pendiente** y la franja ya aparece ocupada en el catálogo.
3. **Autorización (P21).** Admin → *Administración › Institucionales* → autorizar una y rechazar otra con
   motivo → el representante lo ve en su lista (con el motivo).
4. **Eventos (P18, P33, P35).** Admin → *Eventos* → crear un evento en dos instalaciones eligiendo si admite
   externos (las reservas que pisa se cancelan, P7) → *Publicar*. Alumna → *Eventos* → *Inscribirme* (aforo). Admin → *Cancelar evento*: se
   anulan las inscripciones y se liberan las instalaciones.
5. **Escuelas (P19, P20, P22, P39, P44, P45).** Tutora (atajo *Tutora*, o un miembro de la UAL) → *Escuelas* →
   registrar a un menor con teléfono y contacto de emergencia (sin la casilla de consentimiento no se puede) →
   inscribirlo; con una edad fuera de tramo el sistema lo impide. *Retirar consentimiento* borra sus datos. En el
   catálogo, la calle 2 de la piscina aparece *Bloqueada · escuela* lunes y miércoles de 17 a 18.
6. **Entidades (P25, P32).** Admin → *Entidades*: representantes, ausencias y suspensión temporal.
7. **Panel del núcleo común.** *Instalaciones* (alta con el horario general y duración de franja, P12),
   *Usuarios* (alta de un PDI, P16), *Bloqueos y cierres* (cierre por festivo, P11), *Reservas* (consulta
   general y *No presentado*, P3).
8. **Informes (P9).** *Informes* semanal y mensual: ocupación por instalación y por franja, uso por perfil,
   cancelaciones y no presentados.

## Flujo de trabajo

- `main` protegida: solo recibe cambios por pull request revisado por otro integrante.
- Una rama por funcionalidad: `feature/F04-crear-reserva`, `fix/...`, `docs/...`.
- Commits con prefijo: `feat:`, `fix:`, `docs:`, `db:`, `test:`.
- Cada funcionalidad de la Release V1 es un Issue en GitHub Projects, con su criterio de aceptación.

## Documentación

El modelo de datos (MER y modelo relacional) está en `docs/modelo-datos/`. Para regenerar los diagramas
tras cambiar el esquema: `python3 docs/modelo-datos/fuentes/generar_diagramas.py` (requiere Graphviz).
El resto de la documentación del curso (G1–G4) se irá añadiendo a `docs/`.

**Incremento 2 (Variación 7) y seguimiento del proyecto (Hito 2):**

- Contrato de la API de eventos, escuelas, registro y reservas institucionales: [`backend/README.md`](backend/README.md#contrato-de-la-api--incremento-2-variación-7).
- Decisiones pendientes de validar con el cliente (P23–P46): [`docs/requisitos-cliente.md`](docs/requisitos-cliente.md) y [`docs/cliente/2026-11-09_preguntas_incremento2.md`](docs/cliente/2026-11-09_preguntas_incremento2.md).
- Seguimiento y control (Bloque 4): [registro de tareas y sprints](docs/seguimiento/01_registro-tareas-y-sprints.md), [incidencias](docs/seguimiento/02_registro-incidencias.md), [solicitudes de cambio](docs/seguimiento/03_registro-cambios.md) y [métricas](docs/seguimiento/04_metricas-y-control.md).
- Propuesta de los 11 artefactos del expediente: [`docs/hito2/expediente-11-artefactos.md`](docs/hito2/expediente-11-artefactos.md).
