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

## Contrato de la API · Incremento 2 (Variación 7)

`ReservaDto` añade `institucional` (boolean) y `motivoRechazo`. Los estados de reserva incluyen
`RECHAZADA`. Las reglas P23–P46 las validó el cliente el 11/11/2026; las marcadas con ❓ (P47–P60)
siguen pendientes (`docs/requisitos-cliente.md`).

| Método | Ruta | Descripción | Req. |
|---|---|---|---|
| POST | `/api/auth/registro` | **Pública.** `{tipo: TUTOR\|REPRESENTANTE, nombre, apellidos, email, password (≥ 8), telefono (obligatorio para tutores, P24), cifEntidad?}` → 201 `{token, expiraEn, usuario, pendienteValidacion}`. El tutor entra directamente; el representante queda vinculado a la entidad del CIF e **inactivo hasta que la administración lo valida** (P23, sin token; su login da 403 `CUENTA_INACTIVA`) | P17 |
| POST | `/api/reservas-institucionales` | Representante. `{idInstalacion, fecha, horaInicio, horaFin}` → 201 `PENDIENTE`. Varias franjas consecutivas hasta **4 h** (P28), con **48 h** de antelación (P27), convenio vigente (P29) y entidad no suspendida (P32). No cuenta para el límite diario. Se consulta en `/api/reservas/mias` y se cancela con `/api/reservas/{id}/cancelar` | P21 |
| GET | `/api/admin/reservas-institucionales?estado=` | ADMIN. Bandeja con entidad, representante, autorizador y fechas (pendientes primero) | P21 |
| PATCH | `/api/admin/reservas-institucionales/{id}/autorizar` | ADMIN. `PENDIENTE` → `CONFIRMADA`; guarda `id_autorizador` y la fecha (sin plazo, P26) | P21 |
| PATCH | `/api/admin/reservas-institucionales/{id}/rechazar` | ADMIN. `{motivo}` → `RECHAZADA`; la franja queda libre (motivo obligatorio, P30) | P21 |
| GET | `/api/eventos` | Eventos `PUBLICADO` que no han terminado, con `plazasLibres`, `instalaciones` e `inscrito` | P18 |
| GET | `/api/eventos/mios` | Eventos con inscripción activa del usuario | P18 |
| POST / DELETE | `/api/eventos/{id}/inscripcion` | Inscribirse o anular hasta **24 h antes** del inicio (P35). Los externos solo en eventos `COMUNIDAD_Y_EXTERNOS` (P33); la administración no se inscribe | P18 |
| GET / POST | `/api/admin/eventos` | ADMIN. Listar o crear `{nombre, descripcion?, inicio, fin, aforoMaximo, idInstalaciones[], publico: COMUNIDAD_UAL\|COMUNIDAD_Y_EXTERNOS}` → 201 `{evento, reservasCanceladas}`. Crea un bloqueo `EVENTO` por instalación, que cancela las reservas que coinciden (P7) | P18 |
| PATCH | `/api/admin/eventos/{id}/publicar` · `/cancelar` | ADMIN. `PLANIFICADO` → `PUBLICADO` (abre inscripciones). Cancelar anula las inscripciones y libera las instalaciones → `{evento, inscripcionesAnuladas}` | P18 |
| GET | `/api/admin/eventos/{id}/inscritos` | ADMIN. Inscritos con su estado (`ACTIVA` / `ANULADA`) | P18 |
| GET | `/api/escuelas` | Escuelas cuya temporada no ha terminado, con horario, tramo de edad y plazas libres | P19 |
| GET / POST | `/api/menores` | Tutor: externo o miembro de la comunidad UAL (P39). Alta `{nombre, apellidos, fechaNacimiento, telefonoTutor, contactoEmergenciaNombre, contactoEmergenciaTelefono, consentimientoRgpd: true}` → 201 (P22, P44) | P22 |
| DELETE | `/api/menores/{id}` | Tutor. Retira el consentimiento: se eliminan los datos del menor y sus inscripciones (P45) → 204 | P45 |
| POST | `/api/escuelas/{id}/inscripciones` | Tutor. `{idMenor}` → 201. Edad a la fecha de inscripción y plazas, comprobadas por la BD | P20 |
| PATCH | `/api/escuelas/{id}/inscripciones/{idMenor}/baja` | Tutor. Baja del menor; la plaza queda libre (P43) | P19 |
| GET / POST | `/api/admin/escuelas` | ADMIN. Listar o crear `{idInstalacion, nombre, temporada, fechaInicio, fechaFin, edadMin, edadMax, plazas, horario: [{diaSemana 1–7, horaInicio, horaFin}]}` → 201 `{escuela, sesionesBloqueadas, sesionesOmitidas, reservasCanceladas}`. Bloquea la instalación en cada sesión de la temporada; las sesiones que caen en un bloqueo previo (festivos) se omiten. Con reservas confirmadas en sus sesiones → 409 `ESCUELA_CON_RESERVAS` (P41) | P19 |
| GET | `/api/admin/escuelas/{id}/inscritos` | ADMIN. Alumnos con el contacto del tutor | P19 |

Errores nuevos: `EMAIL_EN_USO` (409), `ENTIDAD_OBLIGATORIA` / `ENTIDAD_NO_VALIDA` / `CONVENIO_NO_VIGENTE` (422),
`NO_REPRESENTANTE` (403), `INSTALACION_NO_DISPONIBLE` (409, ya hay un bloqueo), `EVENTO_NO_DISPONIBLE`,
`YA_INSCRITO`, `NO_INSCRITO`, `AFORO_COMPLETO` (`P0003`), `SIN_PLAZAS` (`P0005`) (409),
`EDAD_FUERA_DE_TRAMO` (`P0004`), `EVENTO_EN_PASADO`, `EVENTO_INICIADO`, `TEMPORADA_FINALIZADA`,
`HORARIO_NO_VALIDO`, `EDADES_NO_VALIDAS`, `FECHAS_NO_VALIDAS`, `REPRESENTANTE_NO_VALIDO` (`P0006`) (422),
`MENOR_NO_ENCONTRADO`, `EVENTO_NO_ENCONTRADO`, `ESCUELA_NO_ENCONTRADA` (404).

### Panel de administración del núcleo común (F06, F07, F08, F10) e informes (P9)

Todas las rutas son solo para ADMIN.

| Método | Ruta | Descripción | Req. |
|---|---|---|---|
| GET / POST | `/api/admin/instalaciones` | Todas (incluidas las desactivadas) con horario y reservas futuras · alta `{idTipo, nombre, ubicacion?, aforo, duracionFranjaMin 15–240, descripcion?, horario?: [{diaSemana, horaApertura, horaCierre}]}`. Sin horario se aplica el general de P11 | F06, P11, P12 |
| PUT | `/api/admin/instalaciones/{id}` | Edita datos y duración de franja; `horario` nulo no lo cambia y uno nuevo lo sustituye | F06, P12 |
| PATCH | `/api/admin/instalaciones/{id}/desactivar` · `/activar` | Baja (desactivación) y reactivación. Con reservas futuras → 409 `INSTALACION_CON_RESERVAS` (❓P47) | F06 |
| GET / POST | `/api/admin/usuarios?rol=&texto=` | Lista con filtros · alta `{nombre, apellidos, email, telefono?, rol: ALUMNADO\|PDI\|PAS\|ADMIN, password}` (❓P48) | F07, P16 |
| PATCH | `/api/admin/usuarios/{id}/rol` · `/activar` · `/desactivar` | `{rol}`. Un representante solo puede ser EXTERNO; el admin no se desactiva ni se quita el perfil a sí mismo (❓P49) | F07 |
| GET / POST | `/api/admin/bloqueos?idInstalacion=&desde=&hasta=` | Lista (por defecto, 60 días) · alta `{idInstalacion, inicio, fin, motivo: DOCENCIA\|MANTENIMIENTO\|COMPETICION\|CIERRE\|OTRO, observaciones?}` → `{reservasCanceladas, bloqueo}` | F08, P7 |
| DELETE | `/api/admin/bloqueos/{id}` | 204. Los de eventos y escuelas → 409 `BLOQUEO_GESTIONADO` (❓P50) | F08 |
| POST | `/api/admin/cierres` | `{inicio, fin, observaciones}` → cierre de todas las instalaciones; cubre los huecos alrededor de bloqueos previos (INC-04) | F08, P11 |
| GET | `/api/admin/reservas?desde=&hasta=&idInstalacion=&estado=&texto=` | Consulta general (por defecto, 30 días; máximo 1000 filas) | F10 |
| GET / POST | `/api/admin/entidades` | Entidades con convenio con representantes (y pendientes de validar), ausencias y suspensión · alta `{nombre, cif, emailContacto?, telefono?, fechaInicio, fechaFin?, activa}` | P25, P32 |
| PUT | `/api/admin/entidades/{id}` | Edición | P25 |
| PATCH | `/api/admin/entidades/{id}/suspender` · `/levantar-suspension` | `{hasta, motivo}`. Solo con 3 o más ausencias en el convenio (409 `AUSENCIAS_INSUFICIENTES`) | P32 |
| GET | `/api/admin/informes?periodo=SEMANAL\|MENSUAL&fecha=` | Ocupación por instalación y por franja, uso por perfil, cancelaciones y no presentados (❓P51) | P9 |

Errores nuevos: `INSTALACION_DUPLICADA`, `INSTALACION_CON_RESERVAS`, `BLOQUEO_GESTIONADO` (409);
`TIPO_NO_VALIDO`, `PERFIL_NO_VALIDO`, `OPERACION_SOBRE_SI_MISMO`, `PERIODO_NO_VALIDO` (422);
`CUENTA_INACTIVA`, `ENTIDAD_SUSPENDIDA` (403); `TELEFONO_OBLIGATORIO`, `DURACION_MAXIMA`, `INSCRIPCION_CERRADA` (422);
`ESCUELA_CON_RESERVAS`, `AUSENCIAS_INSUFICIENTES`, `ENTIDAD_DUPLICADA` (409);
`BLOQUEO_NO_ENCONTRADO`, `USUARIO_NO_ENCONTRADO` (404).

Usuarios demo nuevos (contraseña `Sadual2026!`): `tutor.demo@correo.test` (Marta, tutora de Hugo, inscrito
en la *Escuela de natación infantil*, calle 2 de la piscina, L y X 17–18). Evento publicado: *Jornada de
puertas abiertas* el 19/12 (pabellón y sala A).

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

Detalle en [`../docs/requisitos-cliente.md`](../docs/requisitos-cliente.md) (**validadas por el cliente el 25/10/2026**).

| Regla | Dónde se garantiza |
|---|---|
| No se reserva en el pasado; solo franjas del horario de la instalación | `ReservaService` + trigger `tg_validar_reserva` |
| P1 · una reserva activa por usuario y día | `ReservaService` + índice único parcial `uq_reserva_usuario_dia` |
| P2 · al menos 3 horas de antelación, sin máximo | `ReservaService` (`sadual.reservas.antelacion-minima`) |
| P3 · sin reservas el resto de la semana tras no presentarse | trigger `tg_sancionar_no_presentado` + `ReservaService` |
| P4/P17 · reservas individuales solo para la comunidad universitaria (los externos son tutores o representantes) | `PoliticaReservas` (`sadual.reservas.perfiles-permitidos`) |
| P7 · el bloqueo del administrador cancela las reservas que pisa | trigger `tg_bloqueo_cancela_reservas` |
| P11 · festivos y cierres = bloqueos `CIERRE` en todas las instalaciones | función `fn_cerrar_instalaciones` (en el seed: 8/12/2026) |
| P17 · solo un EXTERNO puede representar a una entidad con convenio | columna `usuario.id_entidad` + trigger `tg_validar_representante` |
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
