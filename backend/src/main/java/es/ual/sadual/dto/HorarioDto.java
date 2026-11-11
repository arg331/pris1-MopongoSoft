package es.ual.sadual.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Tramo de apertura semanal de una instalación (diaSemana ISO: 1 = lunes ... 7 = domingo). */
public record HorarioDto(
        @NotNull @Min(value = 1, message = "Día de la semana entre 1 (lunes) y 7 (domingo)")
        @Max(value = 7, message = "Día de la semana entre 1 (lunes) y 7 (domingo)") Integer diaSemana,
        @NotNull(message = "La hora de apertura es obligatoria") LocalTime horaApertura,
        @NotNull(message = "La hora de cierre es obligatoria") LocalTime horaCierre) {
}
