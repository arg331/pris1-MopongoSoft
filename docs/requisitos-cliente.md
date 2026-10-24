# Preguntas al cliente y decisiones de requisitos

Registro de las preguntas P1–P10 planteadas al Servicio de Deportes (G2, apartado 2) y de cómo se
han resuelto.

> **Estado: PROPUESTA DEL EQUIPO, PENDIENTE DE VALIDAR POR EL CLIENTE.**
> La profesora (cliente) todavía no ha respondido. Estas respuestas son la propuesta de MopongoSoft
> y se han enviado al cliente para su validación el __/__/2026. Cuando conteste, se actualiza la columna
> "Validado" y, si algo cambia, se ajusta la configuración indicada (la mayoría de reglas son parámetros).

| # | Pregunta | Respuesta propuesta | Origen | Validado | Dónde se aplica |
|---|---|---|---|---|---|
| P1 | Reservas simultáneas por usuario | **Una reserva activa por día** y usuario. Las reservas institucionales (Variación 7) no cuentan. | Equipo | ☐ | Índice `uq_reserva_usuario_dia` (BD) y `ReservaService` → `409 LIMITE_DIARIO` |
| P2 | Antelación máxima y mínima | **Sin máximo** (mientras la franja esté libre); **mínimo 3 horas**. | Equipo | ☐ | `sadual.reservas.antelacion-minima=PT3H`; franjas `FUERA_DE_PLAZO` en F03; `422 ANTELACION_MINIMA` |
| P3 | Penalización por no presentarse | **Sí: no puede reservar el resto de esa semana.** El administrador marca la reserva como *no presentado* y se crea la sanción desde ese día hasta el domingo. | Equipo | ☐ | Trigger `tg_sancionar_no_presentado`; `PATCH /api/admin/reservas/{id}/no-presentado`; `403 USUARIO_SANCIONADO` |
| P4 | ¿Pueden reservar externos? | **De momento solo la comunidad universitaria** (Alumnado, PDI, PAS). El perfil EXTERNO se mantiene para abrirlo más adelante y para las reservas institucionales del Incremento 2. | Equipo | ☐ | `sadual.reservas.perfiles-permitidos`; `403 PERFIL_NO_AUTORIZADO`; aviso en la pantalla |
| P5 | Lista de espera | **No en esta versión**: se valora como cambio en el Incremento 3 (coherente con G2). | Equipo | ☐ | Fuera de alcance |
| P6 | Pago o tarifa | **Sí, en una fase posterior.** El sistema no procesa pagos (G2: fuera de alcance); como mucho reflejará el estado del cobro. | Equipo | ☐ | Fuera de alcance de V1 |
| P7 | Quién bloquea y prioridad | **Solo el perfil administrador**, por docencia, mantenimiento, competición o evento. **El bloqueo tiene prioridad**: las reservas que pisa quedan canceladas. | Equipo | ☐ | Trigger `tg_bloqueo_cancela_reservas`; rutas `/api/admin/**` solo ADMIN |
| P8 | Datos de usuario y autenticación | **Email y contraseña** en V1 (datos mínimos: nombre, apellidos, email, teléfono opcional y perfil). Autenticación desacoplada para conectar el acceso institucional de la UAL cuando lo permita el Equipo TIC (riesgo R4). | Equipo | ☐ | `TokenService` / `AuthService` |
| P9 | Informes de ocupación | **Todos los que sean viables** → se concretan en el Incremento 3. Propuesta inicial: ocupación por instalación y franja, uso por perfil, cancelaciones y no presentados; consulta semanal y mensual. | Equipo | ☐ | Incremento 3 (vista `v_reservas_detalle` como base) |
| P10 | Dos usuarios reservan la misma franja a la vez | **Solo una reserva gana**; la otra persona recibe un aviso de que la franja acaba de ocuparse. | Equipo | ☐ | Restricción `ex_reserva_sin_solape` (BD) → `409 FRANJA_OCUPADA`; prueba de concurrencia |

## Interpretaciones que conviene confirmar expresamente

- **P3 · "esa semana"**: se ha interpretado como *desde el día de la reserva incumplida hasta el
  domingo de esa semana*; durante ese tiempo el usuario no puede hacer reservas nuevas (para
  ninguna fecha). Las reservas que ya tuviera se mantienen.
- **P7 · prioridad**: al crear un bloqueo se cancelan automáticamente las reservas afectadas. Como no
  hay notificaciones en V1, el usuario lo ve en "Mis reservas" como *Cancelada*.
- **P1 · administrador**: el límite de una reserva al día se aplica también al perfil ADMIN.
