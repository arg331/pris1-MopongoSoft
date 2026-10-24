package es.ual.sadual.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solo se envía la hora de inicio: la hora de fin la calcula el servidor a partir de
 * la duración de franja de la instalación, así no se pueden pedir franjas "a medida".
 */
public record ReservaRequest(
        @NotNull(message = "La instalación es obligatoria") Integer idInstalacion,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio) {
}
