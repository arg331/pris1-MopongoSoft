# Registro de incidencias (Bloque 4 · Anexo A)

Una incidencia es un problema que **ya está ocurriendo**; los problemas potenciales están en la tabla
de riesgos R1–R7 (G2). Responsable del registro: **Abel Rodríguez (Scrum Master)**. Se revisa en cada
Sprint Review. Última actualización: **09/11/2026**.

**Prioridad (Bloque 4, §6.4):** Alta = bloquea el sistema o el avance · Media = afecta en parte ·
Baja = impacto reducido. **Ciclo de vida:** identificación → registro → análisis → resolución →
validación → cierre.

| ID | Fecha | Tipo | Descripción | Prioridad | Responsable | Estado | Fecha resolución |
|---|---|---|---|---|---|---|---|
| INC-01 | Sprint 1 | Técnica | Al lanzar a la vez reservas sobre la misma franja, PostgreSQL abortaba algunas con un *deadlock* (40P01) en lugar de responder «franja ocupada». **Solución:** candado de transacción por instalación (`pg_advisory_xact_lock`) antes de comprobar y escribir. Verificado: 10 peticiones simultáneas → 1 × 201 y 9 × 409, repetido 20 veces. | Alta | CS | Cerrada | 24/10/2026 (commit `472a901`) |
| INC-02 | 09/11/2026 | Organizativa | El código está en `github.com/arg331/pris1-MopongoSoft` (repositorio personal y público). El enunciado exige la organización **`pris1`** con el nombre **`grupo-07`**. `pris1/grupo-07` responde 404: o no existe o no es visible sin permisos. | **Alta** | AR | Abierta | |
| INC-03 | 09/11/2026 | Organizativa | La autoría en GitHub está descompensada y no sigue el flujo pactado: el código del Hito 1 entró en dos commits de una sola persona; dos PR (#3 y #5) los aprobó la misma autora; varios commits no llevan prefijo (`feat:`, `docs:`…). El enunciado usa el historial para verificar el trabajo individual. **Acción:** una rama y un PR por historia, revisión cruzada obligatoria y que cada integrante suba su parte. | Alta | AR | Abierta | |
| INC-04 | 09/11/2026 | Técnica | `fn_cerrar_instalaciones` (P11) omite **toda** una instalación si ya tiene cualquier bloqueo en el periodo. Con las escuelas (bloqueos de una hora), un festivo creado después dejaría la instalación abierta el resto del día. **Solución:** el cierre bloquea los huecos libres alrededor de los bloqueos previos (multirangos de PostgreSQL); verificado con T15. | Media | JZ | En resolución (rama sin PR) | 09/11/2026 |
| INC-05 | 09/11/2026 | Técnica | En el equipo de María Camila hay Java 24 en vez del Java 21 acordado. Compila y pasa las pruebas, pero no es el entorno pactado. | Baja | CS | Abierta | |
| INC-06 | 09/11/2026 | Organizativa | Circulaba una copia local del proyecto anterior al PR #7 (sin las reglas validadas del 25/10), fuera de git. **Solución:** trabajar siempre sobre un clon actualizado de `main`. | Baja | AR | Cerrada | 09/11/2026 |
| INC-07 | 09/11/2026 | Externa | El enunciado pide «11 artefactos en PDF» para el Hito 2 pero no los enumera. Hay que confirmar la lista con la profesora (ver `docs/hito2/expediente-11-artefactos.md`). | Media | AR | Abierta | |
| INC-08 | 09/11/2026 | Externa | El cliente dejó «por definir» el plazo de autorización de las reservas institucionales (P21), y hay otros detalles sin validar: P23–P46 (enviadas el 09/11) y P47–P51. Se implementó la opción más prudente y configurable. **El cliente respondió P23–P46 el 11/11** y se aplicó ese día (CAM-10); quedan abiertas P47–P60. | Media | DA (PO) | En resolución (P47–P60) | 11/11/2026 (P23–P46) |
| INC-09 | 09/11/2026 | Organizativa | Entre el 27/10 y el 08/11 no se integró ningún commit en el repositorio (ninguna rama remota avanza tras el Hito 1). El trabajo del Incremento 2 se concentró el 09/11 y aún no está en `main`. Throughput de 0 historias en dos semanas. **Acción:** objetivos semanales con PR y métricas cada lunes. | Alta | AR | Abierta | |
