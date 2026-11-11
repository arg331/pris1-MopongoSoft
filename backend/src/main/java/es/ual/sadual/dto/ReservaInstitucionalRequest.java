package es.ual.sadual.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * P21 · Reserva institucional. A diferencia de la individual, puede abarcar varias franjas
 * consecutivas: horaInicio debe coincidir con el inicio de una franja y horaFin con el final de otra.
 */
public record ReservaInstitucionalRequest(
        @NotNull(message = "La instalación es obligatoria") Integer idInstalacion,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
}
