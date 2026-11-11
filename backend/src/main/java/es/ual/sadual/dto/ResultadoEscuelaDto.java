package es.ual.sadual.dto;

/**
 * Respuesta al crear una escuela: sesiones bloqueadas en la temporada y sesiones omitidas porque la instalación
 * ya estaba bloqueada (festivos, docencia...). Al cancelarla, reservasCanceladas indica los inscritos dados de baja.
 */
public record ResultadoEscuelaDto(EscuelaDto escuela, int sesionesBloqueadas, int sesionesOmitidas,
                                  int reservasCanceladas) {
}
