package es.ual.sadual.dto;

import jakarta.validation.constraints.NotNull;

public record InscripcionEscuelaRequest(@NotNull(message = "Indica el menor") Integer idMenor) {
}
