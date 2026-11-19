package es.ual.sadual.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Listado de alumnos de una escuela para la administración, con el contacto del tutor (P46) y el contacto
 * de emergencia del menor (P52: el cliente indicó el 18/11/2026 que la administración puede consultarlo).
 */
public record InscritoEscuelaDto(Integer idMenor, String menor, LocalDate fechaNacimiento, String tutor,
                                 String emailTutor, String telefonoTutor, String contactoEmergenciaNombre,
                                 String contactoEmergenciaTelefono, LocalDateTime fechaInscripcion,
                                 String estado) {
}
