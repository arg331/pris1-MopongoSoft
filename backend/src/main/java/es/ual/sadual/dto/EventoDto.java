package es.ual.sadual.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * inscrito: si el usuario que consulta tiene inscripción activa (false en las vistas de administración).
 * publico (P33): COMUNIDAD_UAL o COMUNIDAD_Y_EXTERNOS. cierreInscripcion (P35): 24 h antes del inicio.
 */
public record EventoDto(Integer id, String nombre, String descripcion, LocalDateTime inicio, LocalDateTime fin,
                        int aforoMaximo, String estado, int inscritos, int plazasLibres,
                        List<String> instalaciones, boolean inscrito, String publico,
                        LocalDateTime cierreInscripcion) {
}
