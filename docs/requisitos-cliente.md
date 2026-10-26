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
| P7 | Bloqueos | Solo la administración. **El bloqueo tiene prioridad**: las reservas afectadas se cancelan y el usuario lo ve en "Mis reservas" (sin notificaciones en V1). | ✅ regla en BD · ⏳ pantalla (F08) |
| P8 | Datos y autenticación | Email y contraseña (nombre, apellidos, email, teléfono opcional, perfil). Autenticación desacoplada para conectar el acceso institucional de la UAL (R4). | ✅ |
| P9 | Informes | Incremento 3: ocupación por instalación y franja, uso por perfil, cancelaciones y no presentados; consulta semanal y mensual. | ⏳ Inc. 3 |
| P10 | Concurrencia | Solo una reserva se confirma; la otra persona recibe "la franja ya está reservada". | ✅ |

## B. Núcleo del sistema (P11–P16)

| # | Pregunta | Decisión validada | Estado |
|---|---|---|---|
| P11 | Horario de apertura | L–V 08:00–22:00, sábados 09:00–14:00, domingos cerrado. **Festivos, vacaciones y cierres extraordinarios se gestionan con bloqueos configurables por la administración** (motivo `CIERRE`). | ✅ horario y cierres en BD (`fn_cerrar_instalaciones`, festivo 8/12 en el seed) · ⏳ pantalla (F08) |
| P12 | Duración de las franjas | Pádel 90 min, resto 60 min. **Debe ser configurable por instalación desde el panel de administración.** | ✅ valores y columna `duracion_franja_min` · ⏳ pantalla (F06) |
| P13 | Instalación completa o por plazas | Se reserva la **unidad completa** (pista, calle de piscina, sala…). | ✅ |
| P14 | Cancelación | Hasta que empieza la reserva, sin penalización. | ✅ |
| P15 | Administración / Recepción | Un único perfil de administración que incluye Recepción. | ✅ |
| P16 | Altas de usuarios | La comunidad universitaria la da de alta la administración (sin autorregistro). *Excepción: ver P17.* | ✅ (usuarios del seed) · ⏳ pantalla (F07) |

## C. Variación 7 · Incremento 2 (P17–P22)

| # | Pregunta | Decisión validada | Estado |
|---|---|---|---|
| P17 | Tutores y representantes externos | Los **tutores legales** podrán **registrarse** como usuarios externos para inscribir menores en las escuelas. Los **representantes** podrán registrarse como usuarios externos **vinculados a una entidad con convenio**. Ninguno de los dos hace reservas deportivas individuales de carácter personal. | ✅ sin reservas individuales · ✅ vínculo `usuario.id_entidad` en BD · ⏳ registro (Inc. 2) |
| P18 | Eventos masivos | Los organiza solo la administración; pueden ocupar varias instalaciones; si se cancelan, se anulan las inscripciones. | ⏳ Inc. 2 |
| P19 | Horario de las escuelas | Horario semanal fijo que bloquea la instalación durante la temporada. | ⏳ Inc. 2 |
| P20 | Edad de los menores | A la fecha de inscripción. | ✅ en BD (`tg_validar_inscripcion_escuela`) |
| P21 | Reservas institucionales | Las autoriza la administración (plazo por definir); no cuentan para el límite diario. | 🟡 límite diario ✅ · flujo de autorización ⏳ Inc. 2 |
| P22 | Datos de menores (RGPD) | Registrar el consentimiento del tutor y su fecha. | ⏳ Inc. 2 (añadir al modelo) |

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
