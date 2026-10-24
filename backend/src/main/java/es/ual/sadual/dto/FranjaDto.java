package es.ual.sadual.dto;

import java.time.LocalTime;

/** motivo solo se informa en franjas BLOQUEADA (DOCENCIA, MANTENIMIENTO...). */
public record FranjaDto(LocalTime horaInicio, LocalTime horaFin, EstadoFranja estado, String motivo) {
}
