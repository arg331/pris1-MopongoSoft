package es.ual.sadual.dto;

import java.time.LocalDateTime;

public record InscritoEventoDto(Integer idUsuario, String nombre, String email, String rol,
                                LocalDateTime fechaInscripcion, String estado) {
}
