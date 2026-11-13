package es.ual.sadual.dto;

import java.time.Instant;

/**
 * P17/P23 · Resultado del autorregistro. El tutor queda con la sesión iniciada (token y caducidad); el
 * representante queda pendiente de que la administración compruebe que representa a la entidad y lo
 * active, así que no recibe token (pendienteValidacion = true).
 */
public record RegistroResponse(String token, Instant expiraEn, UsuarioDto usuario, boolean pendienteValidacion) {
}
