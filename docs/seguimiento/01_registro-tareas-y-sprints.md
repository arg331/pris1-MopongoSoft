# Registro de tareas y seguimiento de sprints (Bloque 4 · Anexo C)

Responsable de mantenerlo: **Abel Rodríguez (Scrum Master)**. Se actualiza al cerrar cada tarea y se
revisa en cada Sprint Review. Última actualización: **09/11/2026**.

**Estados (Bloque 4, §5.2):** Pendiente · En desarrollo · Pruebas (en revisión por pull request) ·
Finalizada. Una historia solo pasa a *Finalizada* cuando cumple la **Definition of Done** de G4: está
en `main` por un pull request revisado por otro integrante, funciona contra la BD del script oficial,
cumple su criterio de aceptación y está en el README. Por eso una historia con el backend terminado
pero sin pantalla sigue *En desarrollo*.

Responsables: AR = Abel Rodríguez · DA = Daniel Almendros · CS = María Camila Soto · JZ = Javier Zúñiga.

## 1. Plan de sprints hasta el Hito 2 (avance planificado)

Sprints quincenales según la propuesta técnica (G2), salvo el Sprint 1, de una semana para llegar al
Hito 1. **Propuesta a confirmar en el próximo Sprint Planning.**

| Sprint | Fechas | Objetivo | Historias |
|---|---|---|---|
| 1 | 19/10 – 26/10 | Release V1: entrar, consultar, reservar y cancelar sin dobles reservas | HU-01 … HU-06 |
| 2 | 27/10 – 09/11 | Variación 7: modelo de datos y API | HU-07 … HU-16 (BD y API) |
| 3 | 10/11 – 23/11 | Variación 7: pantallas. Panel de administración del núcleo | HU-07 … HU-16 (pantallas), HU-17 … HU-19 |
| 4 | 24/11 – 07/12 | Consulta general, sanciones, informes (Incremento 3) y pruebas de aceptación | HU-20 … HU-22 |
| — | 08/12 – 14/12 | Congelación: corrección de defectos, expediente (11 PDF), ensayo de la demo | — |

## 2. Product Backlog con estado (registro de tareas)

**SP** = puntos de historia (escala de Fibonacci). Son **una propuesta de estimación** hecha el 09/11
para poder calcular la velocity: el equipo debe revisarlos en el Sprint Planning (planning poker).
Los del Sprint 1 se han estimado a posteriori con la misma escala.

| ID | Historia de usuario | Req. | SP | Resp. | Estado | Inicio | Fin prevista | Fin real | Observaciones |
|---|---|---|---|---|---|---|---|---|---|
| HU-01 | Como usuario quiero iniciar sesión y ver solo las opciones de mi perfil (F01) | P8, P15 | 3 | CS · DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6 |
| HU-02 | Como usuario quiero ver el catálogo de instalaciones filtrado por tipo (F02) | — | 2 | CS · DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6 |
| HU-03 | Como usuario quiero consultar la disponibilidad de un día (F03) | P11 | 5 | CS · JZ · DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6 |
| HU-04 | Como miembro de la UAL quiero reservar una franja sin dobles reservas (F04) | P1–P4, P10, P13 | 8 | CS · JZ · DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6; INC-01 resuelta |
| HU-05 | Como usuario quiero ver y cancelar mis reservas (F05) | P14 | 3 | CS · DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6 |
| HU-06 | Como usuario quiero usar la web desde el móvil (F11) | — | 3 | DA | Finalizada | 19/10 | 26/10 | 24/10 | PR #6 |
| HU-07 | Como tutor legal quiero registrarme para inscribir a mis hijos | P17 | 2 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-08 | Como representante quiero registrarme vinculado a mi entidad | P17, P23 | 3 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-09 | Como representante quiero solicitar una reserva institucional de varias franjas | P21, P27 | 5 | CS · JZ · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-10 | Como administración quiero autorizar o rechazar las reservas institucionales | P21, P26, P30 | 3 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-11 | Como administración quiero crear un evento que ocupe varias instalaciones | P18, P7 | 5 | CS · JZ · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-12 | Como administración quiero publicar y cancelar un evento (anulando inscripciones) | P18, P34 | 3 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-13 | Como usuario quiero inscribirme en un evento sin superar su aforo | P18, P33 | 3 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-14 | Como administración quiero crear una escuela con horario semanal que bloquee la instalación toda la temporada | P19, P42 | 8 | CS · JZ · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-15 | Como tutor quiero registrar a mi hijo dando el consentimiento RGPD | P22 | 2 | CS · JZ · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-16 | Como tutor quiero inscribir a mi hijo en una escuela de su edad | P20, P39, P43 | 3 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-17 | Como administración quiero dar de alta y editar instalaciones y su duración de franja (F06) | P12, P47 | 5 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-18 | Como administración quiero dar de alta a la comunidad UAL y gestionar usuarios (F07) | P16, P48, P49 | 5 | CS · DA | Pruebas | 09/11 | 23/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-19 | Como administración quiero bloquear franjas y cerrar por festivos (F08) | P7, P11, P50 | 5 | CS · JZ · DA | Pruebas | 09/11 | 27/11 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-20 | Como administración quiero consultar todas las reservas con filtros (F10) | — | 3 | CS · DA | Pruebas | 09/11 | 04/12 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-21 | Como administración quiero marcar un no presentado desde el panel (F12) | P3 | 2 | CS · DA | Pruebas | 09/11 | 07/12 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |
| HU-22 | Como Servicio de Deportes quiero informes de ocupación, uso, cancelaciones y no presentados (Inc. 3) | P9, P51 | 8 | CS · JZ · DA | Pruebas | 09/11 | 07/12 |  | Backend y pantalla hechos y probados (09/11); pendiente de PR y revisión |

**Total del alcance del Hito 2:** 22 historias · 89 SP (Sprint 1: 24 · Variación 7: 37 · núcleo e informes: 28).

## 3. Tareas técnicas del Sprint 2 (rama `feature/variacion7-backend`)

| ID | Tarea | Historia | Resp. | Estado | Fecha | Evidencia |
|---|---|---|---|---|---|---|
| T-01 | Modelo de datos v1.2: `RECHAZADA` y datos de resolución en `reserva`; `horario_escuela`; fechas de temporada; motivo `ESCUELA`; consentimiento RGPD; estado de `inscripcion_evento`; edad a la fecha de inscripción | HU-09…16 | CS (revisa JZ) | Pruebas | 09/11 | `database/01_schema.sql` |
| T-02 | `fn_generar_bloqueos_escuela` (bloqueos de toda la temporada) | HU-14 | CS (revisa JZ) | Pruebas | 09/11 | `01_schema.sql` |
| T-03 | Datos de demostración de la Variación 7 (tutora, menor, escuela, evento) | HU-07…16 | CS (revisa JZ) | Pruebas | 09/11 | `database/02_seed.sql` |
| T-04 | Pruebas del modelo T17–T23 | HU-09…16 | CS (revisa JZ) | Pruebas | 09/11 | `03_pruebas.sql`: 21/21 OK |
| T-05 | API de registro de externos | HU-07, HU-08 | CS | Pruebas | 09/11 | `POST /api/auth/registro` |
| T-06 | API de reservas institucionales y bandeja de autorización | HU-09, HU-10 | CS | Pruebas | 09/11 | `/api/reservas-institucionales`, `/api/admin/reservas-institucionales` |
| T-07 | API de eventos | HU-11…13 | CS | Pruebas | 09/11 | `/api/eventos`, `/api/admin/eventos` |
| T-08 | API de escuelas y menores | HU-14…16 | CS | Pruebas | 09/11 | `/api/escuelas`, `/api/menores`, `/api/admin/escuelas` |
| T-09 | Traducción de los errores de los triggers de la Variación 7 (P0003–P0006) | HU-09…16 | CS | Pruebas | 09/11 | `GlobalExceptionHandler` |
| T-10 | Pruebas de integración de la Variación 7 (27 casos) | HU-07…16 | CS | Pruebas | 09/11 | `Variacion7IT`: 103/103 en `mvn verify` |
| T-11 | Pull request de la rama y revisión cruzada (JZ revisa la BD, AR o DA la API) | — | CS | Pendiente | | |
| T-12 | Pantallas de la Variación 7, del panel de administración y de informes (13 pantallas nuevas) | HU-07…22 | CS | Pruebas | 09/11 | 72 pruebas de frontend; recorrido con Chrome sin errores |
| T-13 | Enviar las consultas P23–P46 al cliente | — | DA (PO) | Finalizada | 09/11 | `docs/cliente/2026-11-09_preguntas_incremento2.md` |

| T-14 | API del panel: instalaciones y horarios (F06), usuarios (F07), bloqueos y cierres (F08), consulta general (F10) | HU-17…20 | CS | Pruebas | 09/11 | `AdministracionIT` |
| T-15 | API de informes P9 (ocupación, franjas, perfiles, cancelaciones, no presentados) | HU-22 | CS | Pruebas | 09/11 | `InformeService` |
| T-16 | Corrección de INC-04: el cierre general cubre los huecos libres (T15 del modelo) | HU-19 | CS (revisa JZ) | Pruebas | 09/11 | `fn_cerrar_instalaciones` |
| T-19 | Aplicar las respuestas del cliente a P23–P46 (BD v1.3, API, pantallas y pruebas) | HU-07…16 | CS | Pruebas | 11/11 | CAM-10 · BD 23/23, backend 46 + 128, frontend 76 |
| T-20 | Nueva sección de entidades con convenio en el panel (P25, P32) | HU-08…10 | CS | Pruebas | 11/11 | `AdminEntidadService`, pantalla *Entidades* |
| T-17 | Enviar las consultas P47–P60 al cliente | — | DA (PO) | Pendiente | | `docs/cliente/2026-11-11_consulta_final_P47-P60.md` |
| T-18 | Entrega de la práctica G5 (seguimiento y replanificación) | — | AR (todos) | En desarrollo | 22/11 | `docs/hito2/PRIS1_Grupo07_G5_Seguimiento_SADUAL.docx` |

## 4. Tablero Kanban a 09/11/2026

| Pendiente | En desarrollo | Pruebas / revisión | Finalizada |
|---|---|---|---|
| T-11 · T-17 | T-18 | HU-07 … HU-22 y T-01 … T-16 (todo en la rama, sin PR) | HU-01 … HU-06 |
