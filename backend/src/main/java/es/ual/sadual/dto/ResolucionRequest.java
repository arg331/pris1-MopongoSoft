package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** P21 · Motivo con el que la administración rechaza una reserva institucional. */
public record ResolucionRequest(
        @NotBlank(message = "Indica el motivo del rechazo") @Size(max = 255) String motivo) {
}
