# Funcionalidades priorizadas · Release V1 (MoSCoW)

Responsable: Daniel Almendros Arévalo (Product Owner)

Reglas de negocio validadas por el cliente el 25/10/2026 → [`requisitos-cliente.md`](requisitos-cliente.md).

## Must — imprescindibles para la Release V1 (Hito 1 · 26/10/2026)

| ID | Funcionalidad | Criterio de aceptación | Estado |
|---|---|---|---|
| F01 | Inicio de sesión con perfiles | Login con email y contraseña; el sistema reconoce el perfil (Alumnado, PDI, PAS, Externo, Admin) y muestra solo lo permitido. | ✅ |
| F02 | Catálogo de instalaciones | Listado de instalaciones activas con tipo, ubicación y aforo, filtrable por tipo. | ✅ |
| F03 | Consulta de disponibilidad | Para una instalación y fecha se muestran las franjas libres, ocupadas, bloqueadas (con motivo; los festivos como "Cerrado", P11) y fuera de plazo (P2). | ✅ |
| F04 | Reserva de una franja | Reserva en menos de 1 minuto sobre la unidad completa (P13); franja ocupada, bloqueada o fuera de horario rechazada. Cero dobles reservas (P10). Una reserva por día (P1), al menos 3 h de antelación (P2), solo comunidad universitaria (P4/P17). | ✅ |
| F05 | Mis reservas y cancelación | El usuario ve y cancela sus reservas hasta que empiezan (P14); la franja queda libre al instante. | ✅ |

## Should — importantes pero no críticas (Sprint 2)

| ID | Funcionalidad | Criterio de aceptación | Estado |
|---|---|---|---|
| F06 | Panel admin de instalaciones | Alta, edición y desactivación de instalaciones y horarios. **La duración de franja es configurable por instalación (P12).** | ⏳ (modelo de datos listo) |
| F07 | Gestión básica de usuarios | Alta de la comunidad UAL por la administración (P16), cambiar perfil, activar/desactivar. | ⏳ |
| F08 | Bloqueo de franjas | Bloqueos por docencia, mantenimiento, competición, evento y **cierres (festivos, vacaciones, cierres extraordinarios, P11)**; tienen prioridad y cancelan las reservas afectadas (P7). Reflejados en F03. | 🟡 reglas en BD ✅ · pantalla ⏳ |

## Could — deseables si existe tiempo

| ID | Funcionalidad | Criterio de aceptación | Estado |
|---|---|---|---|
| F09 | Límites por perfil | Límites distintos por perfil (columnas de `rol` preparadas; hoy el límite de P1 es común a todos). | ⏳ |
| F10 | Consulta general de reservas | Filtros por fecha, instalación y estado. | ⏳ (vista `v_reservas_detalle` lista) |
| F11 | Experiencia móvil optimizada | F01–F05 cómodas desde el móvil. | ✅ |
| F12 | Sanción por no presentarse | La administración marca *no presentado* y el usuario no puede hacer reservas nuevas hasta el domingo (P3). | ✅ BD + endpoint · pantalla ⏳ |

## Won't en la V1

- Variación 7 → **Incremento 2**: eventos masivos (P18), escuelas con horario fijo (P19, P20, P22),
  reservas institucionales con autorización (P21) y **registro de tutores y representantes externos (P17)**.
  El modelo de datos ya está preparado, incluido el vínculo representante–entidad (`usuario.id_entidad`).
- Informes de ocupación (P9) → Incremento 3.
- Lista de espera (P5), notificaciones, pago online (P6), app nativa → fuera de alcance (G2).
