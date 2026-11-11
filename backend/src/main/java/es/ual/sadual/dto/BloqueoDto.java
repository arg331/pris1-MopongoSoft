package es.ual.sadual.dto;

import java.time.LocalDateTime;

/** gestionadoPor: EVENTO o ESCUELA si el bloqueo pertenece a esos módulos (no se borra desde F08). */
public record BloqueoDto(Integer id, Integer idInstalacion, String instalacion, LocalDateTime inicio,
                         LocalDateTime fin, String motivo, String observaciones, String creador,
                         String gestionadoPor) {
}
