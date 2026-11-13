package es.ual.sadual.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** P25 · Alta o edición de una entidad con convenio por la administración. */
public record EntidadRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{9}$", message = "El CIF debe tener 9 caracteres") String cif,
        @Email(message = "El email no es válido") @Size(max = 120) String emailContacto,
        @Size(max = 20) String telefono,
        @NotNull(message = "Indica el inicio del convenio") LocalDate fechaInicio,
        LocalDate fechaFin,
        @NotNull Boolean activa) {
}
