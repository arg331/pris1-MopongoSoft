package es.ual.sadual.dto;

import java.time.LocalDate;
import java.util.List;

public record EscuelaDto(Integer id, String nombre, String temporada, LocalDate fechaInicio, LocalDate fechaFin,
                         Integer idInstalacion, String instalacion, int edadMin, int edadMax, int plazas,
                         int inscritos, int plazasLibres, String diasHorario, List<SesionEscuelaDto> horario,
                         String estado) {
}
