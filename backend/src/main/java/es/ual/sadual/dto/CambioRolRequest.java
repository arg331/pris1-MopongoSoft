package es.ual.sadual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambioRolRequest(
        @NotBlank @Pattern(regexp = "ALUMNADO|PDI|PAS|EXTERNO|ADMIN",
                message = "Perfil: ALUMNADO, PDI, PAS, EXTERNO o ADMIN") String rol) {
}
