package es.ual.sadual.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * P22/P45 · Alta de un menor por su tutor: sin consentimiento RGPD no se guarda ningún dato.
 * P44 · Se piden además el teléfono de contacto del tutor y una persona de contacto de emergencia
 * (sin datos médicos en esta versión).
 */
public record MenorRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotNull(message = "La fecha de nacimiento es obligatoria") @Past(message = "La fecha de nacimiento no es válida")
        LocalDate fechaNacimiento,
        @NotBlank(message = "El teléfono de contacto del tutor es obligatorio") @Size(max = 20) String telefonoTutor,
        @NotBlank(message = "Indica una persona de contacto de emergencia") @Size(max = 120) String contactoEmergenciaNombre,
        @NotBlank(message = "Indica el teléfono del contacto de emergencia") @Size(max = 20) String contactoEmergenciaTelefono,
        @NotNull @AssertTrue(message = "Es necesario el consentimiento del tutor para tratar los datos del menor")
        Boolean consentimientoRgpd) {
}
