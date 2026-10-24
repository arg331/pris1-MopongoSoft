package es.ual.sadual.dto;

import java.time.LocalDate;
import java.util.List;

/** Si la instalación no abre ese día, franjas viene vacía. */
public record DisponibilidadDto(Integer idInstalacion, String instalacion, LocalDate fecha,
                                Short duracionFranjaMin, long antelacionMinimaHoras, List<FranjaDto> franjas) {
}
