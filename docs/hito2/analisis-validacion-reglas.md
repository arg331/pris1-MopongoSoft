# Análisis de validación de reglas de negocio (11/11/2026)

**Objetivo.** Comprobar que el sistema no aplica ninguna regla de negocio sin validar por el cliente (el
enunciado penaliza las suposiciones) antes de la última consulta permitida.

**Método.** Se recorrieron todas las reglas que aplica el sistema: los 60 códigos de error de negocio de los
servicios, los `CHECK` y triggers de la base de datos, y los permisos de cada pantalla. Cada regla se clasificó en
tres grupos:

- **V** · validada por el cliente (P1–P46) o por el criterio de aceptación de G4;
- **T** · decisión técnica o de integridad, sin efecto de negocio (no se consulta);
- **❓** · regla de negocio sin validar, que pasa a la consulta final
  ([`../cliente/2026-11-11_consulta_final_P47-P60.md`](../cliente/2026-11-11_consulta_final_P47-P60.md)).

Además se buscaron **huecos funcionales**, es decir, situaciones que el sistema no contempla y en las que el
cliente esperaría una regla (por ejemplo, cancelar una escuela).

## 1. Núcleo común

| Regla del sistema | Respaldo | Tipo |
|---|---|---|
| Una reserva activa por usuario y día (también ADMIN); las institucionales no cuentan | P1, P21 | V |
| Antelación mínima de 3 h para reservas individuales, sin máximo | P2 | V |
| No presentarse impide reservar hasta el domingo; las reservas existentes se mantienen | P3 | V |
| Solo la comunidad universitaria y ADMIN reservan individualmente | P4, P17 | V |
| Solo ADMIN bloquea; el bloqueo cancela las reservas que pisa | P7 | V |
| Email y contraseña; contraseña de al menos 8 caracteres | P8, P24 | V |
| Cero dobles reservas, también en peticiones simultáneas | P10 | V |
| Horario L–V 8–22 y S 9–14 (horario por defecto de una instalación nueva); festivos como cierres | P11 | V |
| Duración de franja configurable por instalación; cambiarla no altera reservas hechas | P12 + interpretación aceptada | V |
| Reserva de la unidad completa | P13 | V |
| Cancelar hasta que empieza, sin penalización | P14 | V |
| Un único perfil ADMIN que incluye recepción | P15 | V |
| La comunidad UAL la da de alta la administración | P16 | V |
| Informes de ocupación, uso por perfil, cancelaciones y no presentados, semanal y mensual | P9 | V (contenido) · ❓P51 (cálculo) |
| Baja de instalación con reservas futuras; reservas fuera de un horario nuevo | — | ❓P47 |
| Contraseña inicial en el alta por la administración | — | ❓P48 |
| Reservas de un usuario desactivado; la administración cancela cualquier reserva | — | ❓P49 |
| Reservas canceladas por un bloqueo que luego se elimina | — | ❓P50 |
| No se pueden solapar dos bloqueos ni crear un evento sobre un bloqueo | — | ❓P54 |
| Formato de email, nombres únicos de instalación, CIF de 9 caracteres, fechas coherentes, no reservar en el pasado, franjas alineadas al inicio de franja | Integridad | T |
| Al entrar con una cuenta inactiva (contraseña correcta) se explica el motivo | Consecuencia de P23 (CAM-11) | T |
| El administrador no puede desactivarse ni quitarse el perfil a sí mismo | Salvaguarda | T |

## 2. Variación 7 · Registro y entidades

| Regla del sistema | Respaldo | Tipo |
|---|---|---|
| Solo se autorregistran tutores y representantes (perfil EXTERNO) | P16, P17 | V |
| El representante indica el CIF de una entidad con convenio vigente y queda pendiente hasta que la administración lo valida | P17, P23, P29 | V |
| Teléfono obligatorio para tutores, opcional para representantes | P24 | V |
| Las entidades las da de alta la administración; varios representantes por entidad | P25 | V |
| Visibilidad de las solicitudes entre representantes de una misma entidad | — | ❓P60 |
| Reservas futuras al terminar o desactivarse el convenio | — | ❓P59 |

## 3. Variación 7 · Reservas institucionales

| Regla del sistema | Respaldo | Tipo |
|---|---|---|
| Nacen pendientes y ocupan la franja; sin plazo de autorización | P21, P26 | V |
| No se autoriza una reserva que ya ha empezado | P26 (propuesta aceptada) | V |
| 48 h de antelación; máximo 4 h seguidas; sin límite semanal; dentro del horario de apertura | P27, P28 (el cliente solo cambió la antelación) | V |
| Solo durante la vigencia del convenio | P29 | V |
| Motivo de rechazo obligatorio, visible para el representante | P30 | V |
| El representante cancela hasta el inicio | P31 | V |
| La ausencia no sanciona al representante; se registra para la entidad; con 3, la administración puede suspender | P32 | V |
| Qué ausencias se consideran justificadas | — | ❓P53 |

## 4. Variación 7 · Eventos

| Regla del sistema | Respaldo | Tipo |
|---|---|---|
| Solo los organiza la administración; varias instalaciones; cancelar anula las inscripciones | P18 | V |
| Público por evento: solo comunidad UAL o comunidad + externos | P33 | V |
| Planificado → publicado; instalaciones bloqueadas desde la creación | P34 | V |
| Una inscripción por persona; inscripción y anulación hasta 24 h antes; no se comprueban solapes entre eventos | P35 + aclaración | V |
| Pueden celebrarse fuera del horario de apertura | P36 | V |
| Un aforo para todo el evento | P37 | V |
| Las reservas canceladas no se recuperan al cancelar el evento | P38 | V |
| La administración no se inscribe | — | ❓P57 |
| Una sanción P3 no impide inscribirse | — | ❓P58 |

## 5. Variación 7 · Escuelas y menores

| Regla del sistema | Respaldo | Tipo |
|---|---|---|
| Horario semanal fijo que bloquea la instalación toda la temporada | P19 | V |
| Edad a la fecha de inscripción; tramos entre 3 y 17 años | P20, P40 | V |
| Consentimiento RGPD obligatorio con fecha; retirarlo elimina los datos y las inscripciones | P22, P45 | V |
| Inscriben tutores externos y la comunidad universitaria | P17, P39 | V |
| No se crea si hay reservas confirmadas en sus sesiones | P41 | V |
| Sesiones en festivos no se generan; un cierre posterior cancela la sesión | P42 | V |
| Inscripción y baja en cualquier momento mientras haya plazas | P43 | V |
| Teléfono del tutor y contacto de emergencia; sin datos médicos | P44 | V |
| La administración ve menor (nombre, nacimiento) y tutor (nombre, email, teléfono) | P46 | V |
| Quién ve y corrige el contacto de emergencia | — | ❓P52 |
| Reservas pendientes en las sesiones de una escuela nueva | — | ❓P55 |
| Sesiones que coinciden con bloqueos que no son festivos (docencia, eventos) | — | ❓P54 |
| Cancelar una escuela ya creada (antes no existía) | Hueco funcional | ❓P56 |

## 6. Resultado

- **Validadas:** todas las reglas de P1–P46.
- **Pendientes:** 14 reglas, reunidas en una única consulta final (P47–P60). En cada una, la **propuesta
  coincide con el comportamiento actual del sistema**, así que una respuesta «Aceptada» no obliga a cambiar
  nada; si el cliente elige la alternativa, el cambio está acotado a la regla correspondiente.
- **Cambios hechos para que esto sea así (11/11):**
  - las reservas pendientes también impiden crear una escuela (P55);
  - nueva cancelación de escuelas (P56);
  - el tutor puede corregir el contacto de emergencia (P52);
  - la pantalla del representante muestra «Mis solicitudes» (P60).
- **Pruebas:** cada propuesta tiene al menos una prueba automática que fija el comportamiento (modelo de datos 23/23,
  backend 46 + 132, frontend 77).
- **Sin consulta, por ser técnicas:** validaciones de formato e integridad, el aviso de cuenta inactiva (CAM-11) y
  las salvaguardas del panel de administración.
