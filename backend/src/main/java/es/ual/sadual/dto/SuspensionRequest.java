package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** P32 · Suspensión temporal de nuevas reservas institucionales de una entidad (hasta la fecha, incluida). */
public record SuspensionRequest(
        @NotNull(message = "Indica hasta cuándo se suspende") LocalDate hasta,
        @NotBlank(message = "Indica el motivo de la suspensión") @Size(max = 255) String motivo) {
}
