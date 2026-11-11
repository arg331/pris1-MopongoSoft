package es.ual.sadual.dto;

import es.ual.sadual.entity.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** P21 · Reserva institucional vista por la administración (bandeja de autorizaciones). */
public record ReservaInstitucionalDto(Integer id, Integer idInstalacion, String instalacion,
                                      LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                      EstadoReserva estado, Integer idEntidad, String entidad,
                                      String representante, String emailRepresentante,
                                      String autorizador, LocalDateTime fechaCreacion,
                                      LocalDateTime fechaResolucion, String motivoRechazo) {
}
