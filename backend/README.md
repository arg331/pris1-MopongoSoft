# Backend · API REST (Spring Boot)

Responsable: María Camila Soto Zapata · Respaldo: Javier Zúñiga Carmona

## Arquitectura

- Java 21 (LTS) y Spring Boot 3.5, proyecto Maven.
- Dependencias: Spring Web, Spring Data JPA, Spring Security + OAuth2 Resource Server (JWT HS256), Validation y driver PostgreSQL.
- Capas: `controller` (endpoints REST) → `service` (reglas de negocio) → `repository` (Spring Data JPA y JDBC) → `entity` (mapeo de las tablas del script).
- El esquema lo crea `../database/01_schema.sql`; JPA trabaja con `spring.jpa.hibernate.ddl-auto=validate`, sin generar tablas.
- La disponibilidad se calcula con la función de BD `fn_franjas_disponibles`, la misma lógica que usan el trigger y la restricción `EXCLUDE`, así API y BD nunca discrepan.
- **Cero dobles reservas, en dos capas**: la API comprueba la franja antes de escribir (mensaje claro) y la restricción `ex_reserva_sin_solape` de PostgreSQL es la última barrera ante peticiones simultáneas; su error `23P01` se traduce a `409`.
- Autenticación sin estado con JWT. La emisión del token está aislada en `TokenService`, de modo que conectar el acceso institucional de la UAL más adelante (riesgo R4) no cambia la API.

## Paquetes

```
es.ual.sadual
├── config/        SecurityConfig (JWT, CORS, BCrypt) · ClockConfig (reloj de negocio)
├── controller/    AuthController, InstalacionController, ReservaController
├── service/       AuthService, TokenService, InstalacionService, DisponibilidadService, ReservaService
├── repository/    Spring Data JPA + FranjaRepository (JDBC sobre fn_franjas_disponibles)
├── entity/        Rol, Usuario, TipoInstalacion, Instalacion, Reserva, EstadoReserva
├── dto/           Peticiones y respuestas de la API (records)
└── exception/     Excepciones de negocio y GlobalExceptionHandler (RFC 7807)
```

## Contrato de la API (Release V1)

Todas las rutas salvo el login exigen `Authorization: Bearer <token>`.

| Método | Ruta | Descripción | Func. |
|---|---|---|---|
| POST | `/api/auth/login` | `{email, password}` → `{token, expiraEn, usuario}` (`usuario.puedeReservar` refleja P4) | F01 |
| POST | `/api/auth/logout` | 204 (JWT sin estado: el cliente descarta el token) | F01 |
| GET | `/api/auth/me` | Usuario autenticado | F01 |
| GET | `/api/tipos-instalacion` | Tipos (para el filtro del catálogo) | F02 |
| GET | `/api/instalaciones?tipo={idTipo}` | Catálogo de instalaciones activas | F02 |
| GET | `/api/instalaciones/{id}` | Detalle de una instalación activa | F02 |
| GET | `/api/instalaciones/{id}/disponibilidad?fecha=YYYY-MM-DD` | Franjas del día con estado `LIBRE`, `RESERVADA`, `BLOQUEADA` (con motivo), `FUERA_DE_PLAZO` (menos de 3 h, P2) o `PASADA` | F03 |
| POST | `/api/reservas` | `{idInstalacion, fecha, horaInicio}` → 201. La hora de fin la calcula el servidor | F04 |
| GET | `/api/reservas/mias` | Reservas del usuario autenticado (más recientes primero) | F05 |
| PATCH | `/api/reservas/{id}/cancelar` | Cancela una reserva propia (el perfil ADMIN, cualquiera) | F05 |
| PATCH | `/api/admin/reservas/{id}/no-presentado` | Solo ADMIN. Marca la reserva como no presentada; la BD crea la sanción (P3) | F12 |

## Errores

Respuestas `application/problem+json` con un `codigo` estable para el frontend:

| HTTP | `codigo` | Cuándo |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Falta un campo o tiene mal formato (incluye `errores` por campo) |
| 401 | `CREDENCIALES_INVALIDAS` | Login incorrecto o usuario desactivado (mismo mensaje en todos los casos) |
| 403 | `RESERVA_AJENA` | Cancelar una reserva de otra persona |
| 403 | `PERFIL_NO_AUTORIZADO` | Un usuario externo intenta reservar (P4) |
| 403 | `USUARIO_SANCIONADO` | Sanción vigente por no presentarse (P3) |
| 409 | `LIMITE_DIARIO` | Ya tiene una reserva ese día (P1; también desde la BD, índice `uq_reserva_usuario_dia`) |
| 404 | `INSTALACION_NO_ENCONTRADA`, `RESERVA_NO_ENCONTRADA` | No existe o está desactivada |
| 409 | `FRANJA_OCUPADA` | Franja ya reservada (incluye la carrera detectada por la BD, `23P01`) |
| 409 | `FRANJA_BLOQUEADA` | Franja bloqueada (`P0001` del trigger) |
| 409 | `RESERVA_NO_CANCELABLE` | La reserva ya estaba cancelada o finalizada |
| 409 | `CONFLICTO_CONCURRENTE` | Red de seguridad ante un deadlock o fallo de serialización de PostgreSQL |
| 400 / 404 / 405 | `PETICION_INVALIDA`, `RUTA_NO_ENCONTRADA`, `METODO_NO_PERMITIDO` | JSON mal formado, parámetro ausente o de tipo incorrecto; ruta o método inexistente |
| 422 | `FRANJA_NO_VALIDA`, `FUERA_DE_HORARIO` | La instalación no abre a esa hora o la hora no es inicio de franja (`P0002`) |
| 422 | `RESERVA_EN_PASADO`, `RESERVA_INICIADA` | Reservar o cancelar una franja que ya ha empezado |
| 422 | `ANTELACION_MINIMA` | Faltan menos de 3 horas para la franja (P2) |
| 409 / 422 | `ESTADO_NO_VALIDO`, `RESERVA_NO_INICIADA` | Marcar *no presentado* algo que no está confirmado o no ha empezado |
| 422 | `DATOS_INCOHERENTES` | Violación de un `CHECK` (`23514`) |

## Reglas de negocio

Detalle y estado en [`../docs/requisitos-cliente.md`](../docs/requisitos-cliente.md) (**propuesta del equipo pendiente de validar por el cliente**).

| Regla | Dónde se garantiza |
|---|---|
| No se reserva en el pasado; solo franjas del horario de la instalación | `ReservaService` + trigger `tg_validar_reserva` |
| P1 · una reserva activa por usuario y día | `ReservaService` + índice único parcial `uq_reserva_usuario_dia` |
| P2 · al menos 3 horas de antelación, sin máximo | `ReservaService` (`sadual.reservas.antelacion-minima`) |
| P3 · sin reservas el resto de la semana tras no presentarse | trigger `tg_sancionar_no_presentado` + `ReservaService` |
| P4 · reservas individuales solo para la comunidad universitaria | `PoliticaReservas` (`sadual.reservas.perfiles-permitidos`) |
| P7 · el bloqueo del administrador cancela las reservas que pisa | trigger `tg_bloqueo_cancela_reservas` |
| P10 · cero dobles reservas | `ReservaService` + restricción `ex_reserva_sin_solape` |

Criterio: las reglas que dependen de "ahora" (P2, P3) se comprueban en el backend con el reloj inyectable; las de integridad (P1, P10) las garantiza además la base de datos, la única capaz de resolver peticiones simultáneas.

**Concurrencia.** Si varias transacciones insertan a la vez reservas que chocan en la restricción `EXCLUDE`, PostgreSQL puede abortar alguna con un *deadlock* (`40P01`): la regla se cumple, pero esa persona recibiría un error. Para evitarlo, `ReservaService` toma un candado de transacción por instalación (`pg_advisory_xact_lock(7001, id_instalacion)`) antes de comprobar y escribir: las reservas de una misma pista se atienden en fila y el perdedor recibe un `409 FRANJA_OCUPADA` limpio; pistas distintas no se esperan entre sí. Si aun así llegara un `40P01`/`40001` por otra vía, se responde `409 CONFLICTO_CONCURRENTE`.

## Configuración

`src/main/resources/application.properties` usa valores de desarrollo que se sobrescriben con variables de entorno:

| Variable | Por defecto |
|---|---|
| `SADUAL_DB_URL` | `jdbc:postgresql://localhost:5433/sadual` (el puerto de `docker-compose.yml`) |
| `SADUAL_DB_USER` / `SADUAL_DB_PASSWORD` | `postgres` / `postgres` (los de `docker-compose.yml`) |
| `SADUAL_JWT_SECRET` | secreto de desarrollo (mínimo 32 caracteres; cambiarlo fuera de desarrollo) |

También se puede crear `application-local.properties` (ignorado por git) y arrancar con `--spring.profiles.active=local`.

## Arranque y pruebas

```bash
mvn spring-boot:run     # http://localhost:8080 (necesita la BD: docker compose up -d en la raíz)
mvn test                # pruebas unitarias (reglas de negocio, traducción de errores), sin Docker
mvn verify              # + pruebas de integración contra PostgreSQL 16 real (Testcontainers, necesita Docker)
```

Las pruebas de integración levantan PostgreSQL con los mismos `01_schema.sql` y `02_seed.sql` del repositorio
y fijan el reloj al 23/10/2026 10:00, así que dan el mismo resultado se ejecuten el día que se ejecuten.
Incluyen dos pruebas de concurrencia: 10 peticiones HTTP simultáneas por la misma franja → 1 × 201 y 9 × 409 (P10),
y una misma persona pidiendo 5 franjas del mismo día a la vez → solo 1 reserva (P1).

> ¿Sin `mvnw`? Generadlo una vez con `mvn -N wrapper:wrapper` (o abrid el proyecto en IntelliJ, que trae Maven).
