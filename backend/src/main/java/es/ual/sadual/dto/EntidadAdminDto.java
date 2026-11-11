package es.ual.sadual.dto;

import java.time.LocalDate;

/**
 * P25 · Entidad con convenio vista por la administración, con sus representantes (activos y pendientes de
 * validar, P23) y las ausencias no justificadas a reservas institucionales durante el convenio (P32).
 * puedeSuspenderse: se han alcanzado las 3 ausencias a partir de las cuales la administración puede suspender.
 */
public record EntidadAdminDto(Integer id, String nombre, String cif, String emailContacto, String telefono,
                              LocalDate fechaInicio, LocalDate fechaFin, boolean activa, int representantes,
                              int representantesPendientes, int ausencias, boolean puedeSuspenderse,
                              LocalDate suspendidaHasta, String motivoSuspension) {
}
