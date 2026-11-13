package es.ual.sadual.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** P19 · Una sesión semanal de una escuela (diaSemana ISO: 1 = lunes ... 7 = domingo). */
public record SesionEscuelaDto(
        @NotNull @Min(value = 1, message = "Día de la semana entre 1 (lunes) y 7 (domingo)")
        @Max(value = 7, message = "Día de la semana entre 1 (lunes) y 7 (domingo)") Integer diaSemana,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
}
