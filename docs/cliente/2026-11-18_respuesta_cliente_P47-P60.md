# Respuesta del cliente a la consulta final P47–P60

**Recibida:** 18/11/2026 · **Asunto:** Respuesta a consulta de requisitos – Variación 7
**De:** Servicio de Deportes de la Universidad de Almería (SAD-UAL), cliente del proyecto

## Texto recibido

> Estimado Grupo 7:
>
> Gracias por el documento remitido.
>
> Tras revisar las cuestiones planteadas, considero que la mayor parte de las preguntas incluidas en esta nueva
> consulta corresponden ya a decisiones de diseño, parametrización o implementación que pueden ser asumidas por el
> equipo sin necesidad de una validación específica por parte del cliente.
>
> Las propuestas presentadas son, en general, coherentes con las decisiones previamente acordadas y con la
> información que ya se ha facilitado durante las entrevistas anteriores.
>
> Por este motivo, salvo indicación expresa en sentido contrario, podéis considerar válidas las soluciones
> propuestas en vuestro documento y continuar con el desarrollo del proyecto.
>
> **Aclaraciones puntuales**
>
> - Los informes podrán visualizarse inicialmente desde la propia aplicación. La exportación a PDF o Excel no se
>   considera obligatoria para esta fase.
> - La información de contacto de emergencia podrá ser consultada por la administración cuando sea necesaria para
>   la gestión de las escuelas deportivas.
> - Las sanciones asociadas a reservas individuales no impedirán la participación en escuelas deportivas ni en
>   eventos institucionales, salvo que una regla futura indique lo contrario.
>
> **Observación del cliente**
>
> En este momento del proyecto resulta más importante consolidar e integrar las funcionalidades ya definidas que
> continuar ampliando el conjunto de reglas de negocio.
>
> A pocos días de la revisión de seguimiento, os recomiendo centrar vuestro esfuerzo en:
>
> - Completar las funcionalidades comprometidas.
> - Validar la integración entre módulos.
> - Mejorar la experiencia de usuario.
> - Comprobar que la variación 7 queda claramente representada dentro de la aplicación.
> - Preparar una demostración funcional estable para la revisión del 23 de noviembre.
>
> A partir de este punto, únicamente sería necesario trasladar nuevas consultas al cliente cuando exista una duda
> que bloquee realmente el desarrollo o implique una decisión de negocio que no pueda ser asumida razonablemente
> por el equipo.
>
> Una vez revisado vuestro documento, considero que disponéis de información suficiente para continuar avanzando
> hacia el Hito 2.

## Efecto sobre el producto

| Pregunta | Decisión del cliente | Estado en la aplicación |
|---|---|---|
| P47–P60 (en general) | Válidas las propuestas del documento del 11/11 | Ya implementadas: dejan de estar pendientes (❓) |
| P51 · Informes | En pantalla; exportar a PDF/Excel no es obligatorio | Coincide con la propuesta |
| P58 · Sanción P3 | No impide escuelas ni eventos | Coincide con la propuesta |
| P52 · Contacto de emergencia | **La administración también puede consultarlo** (se elige la alternativa) | **Implementado el 18/11 (CAM-12):** la lista de inscritos de la administración muestra también el contacto de emergencia |
