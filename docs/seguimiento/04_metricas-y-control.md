# Métricas de seguimiento y acciones de control (Bloque 4 · §4, §10–16)

Corte: **09/11/2026** (fin del Sprint 2 planificado). Fuentes: historial de `main` en GitHub (fechas de
los PR), el registro de tareas ([`01_registro-tareas-y-sprints.md`](01_registro-tareas-y-sprints.md)) y
los resultados de las pruebas. Una historia cuenta como *completada* solo si cumple la Definition of
Done (está en `main` con un PR revisado). Los puntos de historia son la **propuesta de estimación**
del registro de tareas, pendiente de revisar por el equipo.

> **Seguimiento recopila, control decide** (Bloque 4, §3). Cada métrica va con su lectura y, al
> final, con las decisiones que se proponen.

## 1. Avance planificado frente a real (§4)

| Hito / Sprint | Planificado | Real a 09/11 | Desviación |
|---|---|---|---|
| Sprint 1 (19/10–26/10) | F01–F05 (Must) en `main` | F01–F05 + F11 en `main` el 24/10 (PR #6) | Ninguna; dos días de adelanto |
| Sprint 2 (27/10–09/11) | BD y API de la Variación 7 | BD y API terminadas y probadas el 09/11, **en una rama sin PR** | Leve: todo el trabajo se ha concentrado en el último día y aún no está integrado |
| Sprint 3 (10/11–23/11) | Pantallas de la Variación 7 y panel de administración | Sin empezar | — |

## 2. Porcentaje de avance (§11)

`Avance = elementos finalizados ÷ elementos planificados × 100`

| Medida | Finalizados | Total | Avance |
|---|---|---|---|
| Historias de usuario del alcance del Hito 2 | 6 | 22 | **27,3 %** |
| Puntos de historia | 24 | 89 | **27,0 %** |
| Tareas técnicas del Sprint 2 en `main` | 0 | 13 | 0 % |
| Tareas técnicas del Sprint 2 terminadas, pendientes de revisión | 10 | 13 | 77 % |

**Lectura.** La diferencia entre el 77 % y el 0 % es justo la limitación que señala el Bloque 4
(§11.4): el trabajo está hecho, pero no entregado. Mientras no se haga el PR, no cuenta.

## 3. Historias de usuario completadas y cumplimiento del sprint (§12)

| Sprint | Historias previstas | Completadas | Cumplimiento |
|---|---|---|---|
| Sprint 1 | 5 (Must) | 5 (+ F11, que era Could) | **100 %** |
| Sprint 2 | 0 (objetivo técnico: BD y API) | 0 | — · objetivo técnico cumplido al 100 % en la rama |

## 4. Velocity (§13)

`Velocity = suma de los puntos de las historias completadas en el sprint`

| Sprint | Duración | SP completados |
|---|---|---|
| Sprint 1 | 1 semana | 24 |
| Sprint 2 | 2 semanas | 0 |
| **Media** | | **12 SP/sprint** |

**Lectura.** La velocity cae a 0 porque el Sprint 2 se planteó por capas (primero BD y API, después
pantallas): ninguna historia se puede cerrar hasta el Sprint 3. Es una velocity *decreciente* por la
forma de planificar, no por falta de trabajo. **Lección:** cortar las historias en vertical (BD + API
+ pantalla de un mismo módulo) para entregar valor en cada sprint.

## 5. Throughput (§14)

| Semana | Historias completadas |
|---|---|
| 19/10 – 25/10 | 6 |
| 26/10 – 01/11 | 0 |
| 02/11 – 08/11 | 0 |
| **Media** | **2 historias/semana** |

## 6. Lead Time (§15)

`Lead Time = fecha de entrega − fecha de solicitud`

| Elemento | Solicitud | Entrega | Lead Time |
|---|---|---|---|
| HU-01 … HU-06 (Release V1) | 19/10/2026 (backlog de G4) | 24/10/2026 (PR #6) | **5 días** |
| CAM-03 vínculo representante–entidad (BD) | 25/10/2026 (respuesta del cliente) | 26/10/2026 (PR #7) | **1 día** |
| INC-01 deadlock en reservas simultáneas | Sprint 1 | 24/10/2026 | ≤ 5 días |
| HU-07 … HU-16 (Variación 7) | 27/10/2026 (inicio del Incremento 2) | sin entregar | **13 días y contando** |
| Consultas P23–P46 al cliente | 09/11/2026 (envío) | 11/11/2026 (respuesta) | **2 días** |
| CAM-10 (aplicar la respuesta del cliente) | 11/11/2026 | 11/11/2026 (en la rama) | **< 1 día** |

## 7. Ley de Little (§16)

`Lead Time = WIP ÷ Throughput`

| Escenario | WIP | Throughput | Lead Time estimado | Fecha estimada |
|---|---|---|---|---|
| Ritmo medio actual | 16 historias pendientes | 2 HU/semana | **8 semanas** | finales de enero ❌ |
| Ritmo del Sprint 1 | 16 | 6 HU/semana | 2,7 semanas | ~28/11 ✅ |
| **Ritmo necesario** para cerrar el 07/12 | 16 | **4 HU/semana** | 4 semanas | 07/12 ✅ |

**Lectura.** Con el ritmo medio actual no se llega al 14/12. Hay que cerrar **unas 4 historias por
semana** desde hoy. Además, hay 10 historias abiertas a la vez (todas las de la Variación 7). Según la
Ley de Little, tanto trabajo en curso alarga el tiempo de entrega de cada una: conviene cerrarlas
módulo a módulo en lugar de avanzar todas en paralelo.

**Novedad del 09/11.** Ese mismo día quedaron terminados el backend y las pantallas de las 16 historias
pendientes (Variación 7, panel de administración e informes), con 120 pruebas de integración y 72 de
frontend correctas. **Siguen sin contar como finalizadas** porque no están en `main`: el cuello de botella es
ahora la **integración y revisión** (un PR por módulo, revisado por otro integrante) y la validación del
cliente (P23–P51). En cuanto se integren, el avance pasará del 27 % al 100 % de las historias.

## 8. Acciones de control propuestas (para decidir en la Sprint Review)

1. **Integrar esta semana** la rama `feature/variacion7-backend` mediante PR revisado: Javier revisa
   el SQL y Abel o Daniel la API (T-11). Resuelve también parte de INC-03.
2. **Limitar el WIP a 2 módulos.** Pantallas por este orden: (1) registro y reservas institucionales,
   (2) eventos, (3) escuelas y menores. Cada módulo se cierra (PR + DoD) antes de abrir el siguiente.
3. **Reforzar el frontend.** Abel, respaldo de frontend según G4, toma un módulo para que Daniel no
   sea un cuello de botella. María Camila y Javier pueden llevar HU-17…HU-19, cuyo backend es más
   pesado.
4. **Plan de contingencia de alcance (MoSCoW).** El panel de administración (HU-17 alta y baja de
   instalaciones, HU-18 gestión de usuarios y HU-20 consulta general de reservas) es **núcleo común
   obligatorio** del enunciado y no se puede recortar (CAM-09). Si el 23/11 el throughput no llega a
   4 HU/semana, lo que se reduce es HU-22 (informes, Incremento 3) a un único informe de ocupación, y
   HU-21 (pantalla de no presentados) pasa a *Won't*. Hay que avisar al cliente antes (procedimiento de
   cambios).
5. **Enviar ya las consultas P23–P46** para que una respuesta tardía del cliente no bloquee las
   pantallas (riesgo R3).
6. **Medir cada semana.** El Scrum Master actualiza este documento los lunes con los PR fusionados.
