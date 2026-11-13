package es.ual.sadual.dto;

import java.util.List;

/** F06 · Instalación vista desde el panel: incluye las desactivadas, su horario y las reservas futuras. */
public record InstalacionAdminDto(Integer id, String nombre, Short idTipo, String tipo, String ubicacion,
                                  Short aforo, Short duracionFranjaMin, String descripcion, boolean activa,
                                  List<HorarioDto> horario, int reservasFuturas) {
}
