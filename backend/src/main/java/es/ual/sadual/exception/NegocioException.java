package es.ual.sadual.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con código estable (para que el frontend pueda reaccionar)
 * y mensaje en español (para mostrar al usuario).
 */
public abstract class NegocioException extends RuntimeException {

    private final String codigo;

    protected NegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract HttpStatus getStatus();
}
