package es.ual.sadual.dto;

import es.ual.sadual.entity.Usuario;

/**
 * puedeReservar refleja la regla P4 (perfiles con reservas individuales) para que el frontend no la duplique.
 * representante (P17, Incremento 2): el usuario externo está vinculado a una entidad con convenio y puede
 * solicitar reservas institucionales (P21).
 */
public record UsuarioDto(Integer id, String nombre, String apellidos, String email, String rol, String rolNombre,
                         boolean puedeReservar, boolean representante) {

    public static UsuarioDto from(Usuario u, boolean puedeReservar) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellidos(), u.getEmail(),
                u.getRol().getCodigo(), u.getRol().getNombre(), puedeReservar, u.esRepresentante());
    }
}
