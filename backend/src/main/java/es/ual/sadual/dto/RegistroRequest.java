package es.ual.sadual.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * P17 · Autorregistro de usuarios externos. Solo pueden registrarse por su cuenta los tutores
 * legales (para inscribir menores en escuelas) y los representantes de entidades con convenio
 * (indicando el CIF de la entidad). La comunidad UAL la da de alta la administración (P16).
 */
public record RegistroRequest(
        @NotNull(message = "Indica si te registras como tutor o como representante") TipoRegistro tipo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 120) String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @Size(max = 20) String telefono,
        @Pattern(regexp = "^[A-Za-z0-9]{9}$", message = "El CIF debe tener 9 caracteres") String cifEntidad) {

    public enum TipoRegistro { TUTOR, REPRESENTANTE }
}
