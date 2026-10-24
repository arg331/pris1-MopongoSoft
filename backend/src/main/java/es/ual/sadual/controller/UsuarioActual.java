package es.ual.sadual.controller;

import org.springframework.security.oauth2.jwt.Jwt;

/** Lectura del usuario autenticado a partir del JWT (sub = id_usuario, rol = código de perfil). */
final class UsuarioActual {

    private UsuarioActual() {
    }

    static Integer id(Jwt jwt) {
        return Integer.valueOf(jwt.getSubject());
    }

    static boolean esAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("rol"));
    }
}
