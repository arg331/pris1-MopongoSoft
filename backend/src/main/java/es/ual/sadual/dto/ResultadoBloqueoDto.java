package es.ual.sadual.dto;

/** bloqueosCreados > 1 en un cierre general (uno por hueco y por instalación). */
public record ResultadoBloqueoDto(int bloqueosCreados, int reservasCanceladas, BloqueoDto bloqueo) {
}
