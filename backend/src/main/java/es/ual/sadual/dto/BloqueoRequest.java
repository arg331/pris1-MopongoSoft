package es.ual.sadual.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * F08 · P7: bloqueo de una instalación por la administración. Los bloqueos EVENTO y ESCUELA no se
 * crean aquí: los generan los módulos de eventos (P18) y escuelas (P19).
 */
public record BloqueoRequest(
        @NotNull(message = "La instalación es obligatoria") Integer idInstalacion,
        @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @NotNull @Pattern(regexp = "DOCENCIA|MANTENIMIENTO|COMPETICION|CIERRE|OTRO",
                message = "Motivo: DOCENCIA, MANTENIMIENTO, COMPETICION, CIERRE u OTRO") String motivo,
        @Size(max = 255) String observaciones) {
}
