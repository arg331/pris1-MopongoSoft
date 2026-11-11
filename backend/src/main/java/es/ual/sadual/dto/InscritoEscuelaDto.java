package es.ual.sadual.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Listado de alumnos de una escuela para la administración, con el contacto del tutor. */
public record InscritoEscuelaDto(Integer idMenor, String menor, LocalDate fechaNacimiento, String tutor,
                                 String emailTutor, String telefonoTutor, LocalDateTime fechaInscripcion,
                                 String estado) {
}
