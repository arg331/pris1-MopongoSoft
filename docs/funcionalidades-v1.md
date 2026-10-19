# Funcionalidades priorizadas · Release V1 (MoSCoW)

Responsable: Daniel Almendros Arévalo (Product Owner)

## Must — imprescindibles para la Release V1

| ID | Funcionalidad | Criterio de aceptación |
|---|---|---|
| F01 | Inicio de sesión con perfiles | Login con email y contraseña; el sistema reconoce el perfil (Alumnado, PDI, PAS, Externo, Admin) y muestra solo lo permitido. |
| F02 | Catálogo de instalaciones | Listado de instalaciones activas con tipo, ubicación y aforo, filtrable por tipo. |
| F03 | Consulta de disponibilidad | Para una instalación y fecha se muestran las franjas libres y ocupadas (incluidos bloqueos). |
| F04 | Reserva de una franja | Reserva en menos de 1 minuto; franja ocupada, bloqueada o fuera de horario rechazada. Cero dobles reservas. |
| F05 | Mis reservas y cancelación | El usuario ve y cancela sus reservas; la franja queda libre al instante. |

## Should — importantes pero no críticas

| ID | Funcionalidad | Criterio de aceptación |
|---|---|---|
| F06 | Panel admin de instalaciones | Alta, edición y desactivación de instalaciones y horarios. |
| F07 | Gestión básica de usuarios | Listar, cambiar perfil, activar/desactivar. |
| F08 | Bloqueo de franjas | Bloqueo por docencia o mantenimiento reflejado en F03. |

## Could — deseables si existe tiempo

| ID | Funcionalidad | Criterio de aceptación |
|---|---|---|
| F09 | Límites por perfil | Máximo semanal y antelación por perfil (valores provisionales hasta P1/P2). |
| F10 | Consulta general de reservas | Filtros por fecha, instalación y estado. |
| F11 | Experiencia móvil optimizada | F01–F05 cómodas desde el móvil. |

## Won't en la V1

- Variación 7 (eventos masivos, escuelas, reservas institucionales) → Incremento 2 (modelo de datos ya preparado).
- Sanciones → pendiente de la respuesta P3 del cliente.
- Lista de espera, notificaciones, pago online, app nativa → fuera de alcance (G2).
