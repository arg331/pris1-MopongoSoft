package es.ual.sadual.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** F10 · Fila de la consulta general de reservas (vista v_reservas_detalle). */
public record ReservaAdminDto(Integer id, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String estado,
                              Integer idInstalacion, String instalacion, String tipo, Integer idUsuario,
                              String usuario, String rol, String entidad) {
}
