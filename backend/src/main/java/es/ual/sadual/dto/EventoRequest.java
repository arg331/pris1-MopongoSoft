package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

/** P18 · Alta de un evento masivo por la administración. Puede ocupar varias instalaciones. */
public record EventoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        String descripcion,
        @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @NotNull(message = "El aforo es obligatorio") @Positive(message = "El aforo debe ser mayor que 0") Integer aforoMaximo,
        @NotEmpty(message = "Indica al menos una instalación") Set<@NotNull Integer> idInstalaciones,
        /** P33: COMUNIDAD_UAL (solo comunidad universitaria) o COMUNIDAD_Y_EXTERNOS. */
        @NotNull(message = "Indica quién puede inscribirse")
        @Pattern(regexp = "COMUNIDAD_UAL|COMUNIDAD_Y_EXTERNOS", message = "Público: COMUNIDAD_UAL o COMUNIDAD_Y_EXTERNOS")
        String publico) {
}
