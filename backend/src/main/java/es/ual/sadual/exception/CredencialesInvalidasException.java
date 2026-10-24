package es.ual.sadual.exception;

import org.springframework.http.HttpStatus;

/** Mismo mensaje para email inexistente, contraseña errónea o usuario inactivo (no revela qué falló). */
public class CredencialesInvalidasException extends NegocioException {

    public CredencialesInvalidasException() {
        super("CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
