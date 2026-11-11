package es.ual.sadual.dto;

/**
 * Respuesta de crear o cancelar un evento: además del evento, cuántas reservas se han cancelado
 * por sus bloqueos (P7) y cuántas inscripciones se han anulado al cancelarlo (P18).
 */
public record ResultadoEventoDto(EventoDto evento, int reservasCanceladas, int inscripcionesAnuladas) {
}
