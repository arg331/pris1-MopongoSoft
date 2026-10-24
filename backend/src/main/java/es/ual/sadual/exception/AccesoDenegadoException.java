package es.ual.sadual.exception;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
