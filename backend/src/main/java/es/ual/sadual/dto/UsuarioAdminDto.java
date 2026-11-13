package es.ual.sadual.dto;

import java.time.LocalDateTime;

/** F07 · Usuario visto desde el panel de administración. */
public record UsuarioAdminDto(Integer id, String nombre, String apellidos, String email, String telefono,
                              String rol, String rolNombre, boolean activo, LocalDateTime fechaAlta,
                              Integer idEntidad, String entidad, int reservasFuturas) {
}
