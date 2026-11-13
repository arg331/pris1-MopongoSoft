package es.ual.sadual.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * F06 · Alta o edición de una instalación. P12: la duración de franja se configura por instalación.
 * Si no se envía horario al crearla, se aplica el horario general validado en P11
 * (lunes a viernes 08:00-22:00 y sábados 09:00-14:00). Al editar, un horario nulo no lo cambia.
 */
public record InstalacionRequest(
        @NotNull(message = "El tipo es obligatorio") Short idTipo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @Size(max = 120) String ubicacion,
        @NotNull @Min(value = 1, message = "El aforo debe ser mayor que 0") @Max(32767) Integer aforo,
        @NotNull @Min(value = 15, message = "La franja dura entre 15 y 240 minutos")
        @Max(value = 240, message = "La franja dura entre 15 y 240 minutos") Integer duracionFranjaMin,
        @Size(max = 255) String descripcion,
        List<@Valid @NotNull HorarioDto> horario) {
}
