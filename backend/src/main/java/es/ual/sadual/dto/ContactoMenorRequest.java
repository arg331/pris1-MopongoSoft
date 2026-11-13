package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ❓P52 · Corrección por el tutor del teléfono y del contacto de emergencia del menor (P44). */
public record ContactoMenorRequest(
        @NotBlank(message = "El teléfono de contacto del tutor es obligatorio") @Size(max = 20) String telefonoTutor,
        @NotBlank(message = "Indica una persona de contacto de emergencia") @Size(max = 120) String contactoEmergenciaNombre,
        @NotBlank(message = "Indica el teléfono del contacto de emergencia") @Size(max = 20) String contactoEmergenciaTelefono) {
}
