package es.ual.sadual.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MenorDto(Integer id, String nombre, String apellidos, LocalDate fechaNacimiento,
                       LocalDateTime fechaConsentimiento, String telefonoTutor, String contactoEmergenciaNombre,
                       String contactoEmergenciaTelefono, List<InscripcionMenorDto> inscripciones) {

    public record InscripcionMenorDto(Integer idEscuela, String escuela, String temporada,
                                      LocalDateTime fechaInscripcion, String estado) {
    }
}
