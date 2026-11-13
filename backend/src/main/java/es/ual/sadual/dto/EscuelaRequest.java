package es.ual.sadual.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** P19 · Alta de una escuela con su horario semanal fijo para toda la temporada. */
public record EscuelaRequest(
        @NotNull(message = "La instalación es obligatoria") Integer idInstalacion,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotNull @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$", message = "La temporada tiene el formato 2026-2027") String temporada,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        @NotNull @Min(value = 3, message = "La edad mínima es 3 años") Integer edadMin,
        @NotNull @Max(value = 17, message = "La edad máxima es 17 años") Integer edadMax,
        @NotNull @Positive(message = "Las plazas deben ser más de 0") Integer plazas,
        @NotEmpty(message = "Indica al menos una sesión semanal") List<@Valid @NotNull SesionEscuelaDto> horario) {
}
