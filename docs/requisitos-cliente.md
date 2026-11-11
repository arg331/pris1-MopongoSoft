# Requisitos validados por el cliente (P1–P22)

Registro de las preguntas planteadas al Servicio de Deportes y de su resolución.

> **Estado: VALIDADO POR EL CLIENTE el 25/10/2026.**
> El equipo envió sus propuestas el 24/10/2026 ([`cliente/2026-10-24_propuestas_requisitos_P1-P22.docx`](cliente/2026-10-24_propuestas_requisitos_P1-P22.docx)).
> El cliente (profesora) respondió el 25/10/2026 ([`cliente/2026-10-25_respuesta_cliente_requisitos.pdf`](cliente/2026-10-25_respuesta_cliente_requisitos.pdf)):
> *"Tras revisar las propuestas planteadas, se consideran aceptadas por el cliente para esta fase del proyecto.
> Podéis utilizarlas como reglas de negocio válidas […]"*. Además detalló P11, P12, P13 y P17.
> Cualquier modificación posterior se tratará como cambio de requisitos (procedimiento de gestión del alcance, riesgo R2).

Leyenda del estado: ✅ implementado en la Release V1 · 🟡 parcial · ⏳ planificado (incremento indicado) · ⛔ fuera de alcance.

## A. Preguntas de la G2 (P1–P10)

| # | Pregunta | Decisión validada | Estado |
|---|---|---|---|
| P1 | Reservas por usuario | **Una reserva activa por día** y usuario, también para el perfil de administración. Las reservas institucionales no cuentan. | ✅ |
| P2 | Antelación | **Mínimo 3 horas**, sin máximo mientras la franja esté libre. | ✅ |
| P3 | Penalización por no presentarse | La administración marca la reserva como *no presentado* y el usuario **no puede hacer reservas nuevas desde ese día hasta el domingo**. Sus reservas existentes se mantienen. | ✅ |
| P4 | Usuarios externos | **Solo la comunidad universitaria** (alumnado, PDI, PAS) hace reservas individuales. Ver P17. | ✅ |
| P5 | Lista de espera | No en esta versión; se valorará en el Incremento 3. | ⛔ V1 |
| P6 | Pago | Fase posterior. El sistema no procesa pagos; como mucho reflejará si una reserva está pagada. | ⛔ V1 |
| P7 | Bloqueos | Solo la administración. **El bloqueo tiene prioridad**: las reservas afectadas se cancelan y el usuario lo ve en "Mis reservas" (sin notificaciones en V1). | ✅ regla en BD · ✅ pantalla (rama pendiente de PR) |
| P8 | Datos y autenticación | Email y contraseña (nombre, apellidos, email, teléfono opcional, perfil). Autenticación desacoplada para conectar el acceso institucional de la UAL (R4). | ✅ |
| P9 | Informes | Incremento 3: ocupación por instalación y franja, uso por perfil, cancelaciones y no presentados; consulta semanal y mensual. | ✅ API y pantalla (rama pendiente de PR) · ❓ cálculo (P51) |
| P10 | Concurrencia | Solo una reserva se confirma; la otra persona recibe "la franja ya está reservada". | ✅ |

## B. Núcleo del sistema (P11–P16)

| # | Pregunta | Decisión validada | Estado |
|---|---|---|---|
| P11 | Horario de apertura | L–V 08:00–22:00, sábados 09:00–14:00, domingos cerrado. **Festivos, vacaciones y cierres extraordinarios se gestionan con bloqueos configurables por la administración** (motivo `CIERRE`). | ✅ horario y cierres en BD (`fn_cerrar_instalaciones`, festivo 8/12 en el seed) · ✅ pantalla (rama pendiente de PR) |
| P12 | Duración de las franjas | Pádel 90 min, resto 60 min. **Debe ser configurable por instalación desde el panel de administración.** | ✅ valores y columna `duracion_franja_min` · ✅ pantalla (rama pendiente de PR) |
| P13 | Instalación completa o por plazas | Se reserva la **unidad completa** (pista, calle de piscina, sala…). | ✅ |
| P14 | Cancelación | Hasta que empieza la reserva, sin penalización. | ✅ |
| P15 | Administración / Recepción | Un único perfil de administración que incluye Recepción. | ✅ |
| P16 | Altas de usuarios | La comunidad universitaria la da de alta la administración (sin autorregistro). *Excepción: ver P17.* | ✅ (usuarios del seed) · ✅ pantalla (rama pendiente de PR) |

## C. Variación 7 · Incremento 2 (P17–P22)

| # | Pregunta | Decisión validada | Estado |
|---|---|---|---|
| P17 | Tutores y representantes externos | Los **tutores legales** podrán **registrarse** como usuarios externos para inscribir menores en las escuelas. Los **representantes** podrán registrarse como usuarios externos **vinculados a una entidad con convenio**. Ninguno de los dos hace reservas deportivas individuales de carácter personal. | ✅ API `POST /api/auth/registro` (Inc. 2) · ✅ pantalla (rama pendiente de PR) |
| P18 | Eventos masivos | Los organiza solo la administración; pueden ocupar varias instalaciones; si se cancelan, se anulan las inscripciones. | ✅ API y BD (un bloqueo `EVENTO` por instalación; inscripciones `ANULADA`) · ✅ pantalla (rama pendiente de PR) |
| P19 | Horario de las escuelas | Horario semanal fijo que bloquea la instalación durante la temporada. | ✅ API y BD (`horario_escuela`, bloqueos `ESCUELA`, `fn_generar_bloqueos_escuela`) · ✅ pantalla (rama pendiente de PR) |
| P20 | Edad de los menores | A la fecha de inscripción. | ✅ en BD (`tg_validar_inscripcion_escuela`, ahora con `fecha_inscripcion`) |
| P21 | Reservas institucionales | Las autoriza la administración (plazo por definir); no cuentan para el límite diario. | ✅ API (solicitud, autorización y rechazo) · ❓ plazo (P26) · ✅ pantalla (rama pendiente de PR) |
| P22 | Datos de menores (RGPD) | Registrar el consentimiento del tutor y su fecha. | ✅ `menor.consentimiento_rgpd` y `fecha_consentimiento` (obligatorio) · ✅ pantalla (rama pendiente de PR) |

## Interpretaciones del equipo (sin consulta adicional, según indica el cliente)

- **P16 y P17.** Solo pueden registrarse por su cuenta los usuarios externos (tutores y representantes).
  Los miembros de la comunidad UAL los da de alta la administración.
- **P17 · representante.** Solo un usuario con perfil EXTERNO puede estar vinculado a una entidad
  (lo garantiza el trigger `tg_validar_representante`). Un tutor no necesita entidad.
- **P11 · cierre general.** Un cierre crea un bloqueo `CIERRE` en cada instalación activa. Las que ya
  tienen un bloqueo en ese periodo se omiten (siguen bloqueadas por el existente). Por P7, el cierre
  cancela las reservas que coincidan.
- **P12 · cambio de duración.** Cambiar la duración de franja no altera las reservas ya hechas, que
  conservan su hora de inicio y fin (a tener en cuenta al construir F06).

## D. Incremento 2 · decisiones del cliente del 11/11/2026 (P23–P46) y consulta final (P47–P60)

El equipo envió las consultas el 09/11 ([`cliente/2026-11-09_preguntas_incremento2.md`](cliente/2026-11-09_preguntas_incremento2.md))
y el cliente respondió el 11/11 ([`cliente/2026-11-11_respuesta_cliente_P23-P46.md`](cliente/2026-11-11_respuesta_cliente_P23-P46.md)).
Las decisiones son reglas oficiales y están implementadas en la rama `feature/variacion7-backend`.

| # | Tema | Decisión del cliente | Implementación | Estado |
|---|---|---|---|---|
| P23 | Alta del representante | Pendiente hasta que la administración compruebe que representa a la entidad y la active | Se registra inactivo; login → 403 `CUENTA_INACTIVA`; la administración lo valida en *Usuarios* (filtro de pendientes) | ✅ |
| P24 | Datos del registro | Teléfono obligatorio para tutores, opcional para representantes | 422 `TELEFONO_OBLIGATORIO` | ✅ |
| P25 | Entidades | Las da de alta la administración; varios representantes por entidad | Sección *Entidades* del panel | ✅ |
| P26 | Plazo de autorización | Pendiente hasta que la administración resuelva | Sin plazo automático | ✅ |
| P27 | Antelación de las institucionales | Mínimo 48 h | `sadual.institucionales.antelacion-minima=PT48H` | ✅ |
| P28 | Duración y límites | Máximo 4 h seguidas; sin límite semanal con convenio vigente | 422 `DURACION_MAXIMA` | ✅ |
| P29 | Vigencia del convenio | Solo durante el convenio | 422 `CONVENIO_NO_VIGENTE` | ✅ |
| P30 | Rechazo | Motivo obligatorio | — | ✅ |
| P31 | Cancelación | El representante cancela hasta el inicio | — | ✅ |
| P32 | No presentado en una institucional | Se registra para la entidad; con 3 ausencias no justificadas en el convenio, la administración puede suspender temporalmente las nuevas reservas | Sin sanción al representante (trigger); ausencias por entidad; suspensión manual con fecha y motivo (403 `ENTIDAD_SUSPENDIDA`) | ✅ · ❓P53 |
| P33 | Inscripción en eventos | La administración elige por evento: solo comunidad UAL o comunidad + externos | Campo `evento.publico`; los externos solo ven los eventos abiertos a ellos | ✅ |
| P34 | Bloqueo de instalaciones | Desde la creación del evento | — | ✅ |
| P35 | Plazos de inscripción | Una inscripción por persona; inscripción y anulación hasta 24 h antes | `sadual.eventos.cierre-inscripcion=PT24H` (422 `INSCRIPCION_CERRADA`) | ✅ |
| P36 | Eventos fuera de horario | Permitidos cuando los autoriza (crea) la administración | — | ✅ |
| P37 | Aforo | Uno por evento | — | ✅ |
| P38 | Cancelación del evento | No se recuperan las reservas canceladas | — | ✅ |
| P39 | Quién inscribe menores | También la comunidad universitaria que actúe como tutor legal | `sadual.escuelas.perfiles-tutor=ALUMNADO,PDI,PAS,EXTERNO` | ✅ |
| P40 | Tramos de edad | 3–17 años | — | ✅ |
| P41 | Escuela con reservas | No se crea si hay reservas confirmadas en sus franjas | 409 `ESCUELA_CON_RESERVAS` | ✅ |
| P42 | Cierres y escuelas | Un cierre posterior cancela la sesión de la escuela (sin recuperarla) | `fn_cerrar_instalaciones` borra esas sesiones (prueba T24) | ✅ |
| P43 | Inscripciones y bajas | En cualquier momento mientras haya plazas | — | ✅ |
| P44 | Datos del menor | Además: teléfono del tutor y contacto de emergencia; sin datos médicos | Columnas nuevas en `menor` | ✅ · ❓P52 |
| P45 | Consentimiento RGPD | Obligatorio; si se retira se eliminan los datos y se cancelan las inscripciones; texto legal de la UAL | `DELETE /api/menores/{id}` (eliminación); texto provisional en pantalla | ✅ |
| P46 | Datos visibles para la administración | Menor (nombre, fecha de nacimiento) y tutor (nombre, email, teléfono) | — | ✅ |

**Consulta final** ([`cliente/2026-11-11_consulta_final_P47-P60.md`](cliente/2026-11-11_consulta_final_P47-P60.md)). Tras revisar todas las reglas que aplica el sistema frente a
P1–P46 (análisis en [`hito2/analisis-validacion-reglas.md`](hito2/analisis-validacion-reglas.md)), estas son las
**únicas** que quedan sin validar. Cada propuesta describe el comportamiento actual del sistema.

| # | Tema | Propuesta implementada (provisional) | Estado |
|---|---|---|---|
| P47 | Baja de una instalación con reservas futuras; cambio de horario | No se permite la baja; las reservas fuera del nuevo horario se mantienen | ❓ |
| P48 | Alta de la comunidad UAL | Contraseña inicial fijada por la administración; sin cambio obligatorio ni alta masiva | ❓ |
| P49 | Usuario desactivado | No puede entrar; sus reservas se mantienen; la administración puede cancelar cualquier reserva | ❓ |
| P50 | Eliminar un bloqueo | La franja se libera; las reservas canceladas no se recuperan | ❓ |
| P51 | Cálculo de los informes (P9) | Ocupación = horas reservadas ÷ horas abiertas no bloqueadas; semana L–D y mes natural; sin exportación | ❓ |
| P52 | Contacto de emergencia (P44) | Lo ve y corrige el tutor; la administración ve lo de P46 | ❓ |
| P53 | Ausencias justificadas (P32) | Solo se marcan «no presentado» las no justificadas | ❓ |
| P54 | Actividad sobre un bloqueo existente | No se pisan bloqueos (evento o bloqueo rechazado; sesiones de escuela omitidas); solo el cierre general se impone (P42) | ❓ |
| P55 | Reservas pendientes al crear una escuela (P41) | También impiden crearla | ❓ |
| P56 | Cancelar una escuela | Se liberan sus sesiones futuras y los inscritos pasan a baja; el horario no se edita | ❓ |
| P57 | Administración en eventos | No se inscribe | ❓ |
| P58 | Sanción P3 y eventos/escuelas | La sanción solo afecta a reservas individuales | ❓ |
| P59 | Convenio terminado o entidad desactivada | Sin nuevas reservas; las ya autorizadas se mantienen y la administración puede cancelarlas | ❓ |
| P60 | Varios representantes por entidad (P25) | Cada uno ve y cancela solo sus solicitudes; la administración ve todas | ❓ |
