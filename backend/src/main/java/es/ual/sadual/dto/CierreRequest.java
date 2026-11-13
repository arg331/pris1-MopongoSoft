package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** P11 · Cierre de todas las instalaciones (festivo, vacaciones, cierre extraordinario). */
public record CierreRequest(
        @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @NotBlank(message = "Indica el motivo del cierre (p. ej. «Festivo: Día de Andalucía»)") @Size(max = 255)
        String observaciones) {
}
