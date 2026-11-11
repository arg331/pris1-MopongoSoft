package es.ual.sadual.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * F07 · P16: la administración da de alta a la comunidad universitaria (y a otros administradores).
 * Los externos no se dan de alta aquí: se registran ellos mismos (P17).
 * La contraseña inicial la fija la administración (❓P48: entrega y cambio en el primer acceso).
 */
public record AltaUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 120) String email,
        @Size(max = 20) String telefono,
        @NotBlank @Pattern(regexp = "ALUMNADO|PDI|PAS|ADMIN", message = "Perfil: ALUMNADO, PDI, PAS o ADMIN") String rol,
        @NotBlank(message = "La contraseña inicial es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {
}
