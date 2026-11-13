package es.ual.sadual.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce errores a respuestas RFC 7807 (application/problem+json) con un "codigo" estable.
 * <p>
 * La API valida antes de escribir, pero la base de datos es la última barrera: si dos
 * peticiones llegan a la vez, una de ellas choca con ex_reserva_sin_solape (23P01) y aquí
 * se convierte en un 409 limpio en lugar de un 500. Lo mismo con el límite de una reserva
 * por día (índice uq_reserva_usuario_dia, 23505).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail negocio(NegocioException ex) {
        return problema(ex.getStatus(), ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    public ProblemDetail baseDeDatos(RuntimeException ex) {
        SQLException sql = causaSql(ex);
        String sqlState = sql == null ? null : sql.getSQLState();
        if (sqlState == null) {
            log.error("Error de acceso a datos no traducible", ex);
            return problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error interno del servidor");
        }
        String mensajeSql = String.valueOf(sql.getMessage());
        return switch (sqlState) {
            case "23P01" -> mensajeSql.contains("ex_bloqueo_sin_solape")
                    ? problema(HttpStatus.CONFLICT, "INSTALACION_NO_DISPONIBLE",
                            "La instalación ya está bloqueada en ese periodo")
                    : problema(HttpStatus.CONFLICT, "FRANJA_OCUPADA",
                            "La franja acaba de ser reservada por otra persona");
            case "P0001" -> problema(HttpStatus.CONFLICT, "FRANJA_BLOQUEADA",
                    "La instalación está bloqueada en esa franja");
            case "P0002" -> problema(HttpStatus.UNPROCESSABLE_ENTITY, "FUERA_DE_HORARIO",
                    "La franja solicitada está fuera del horario de apertura");
            // Variación 7 (Incremento 2): reglas que garantizan los triggers
            case "P0003" -> problema(HttpStatus.CONFLICT, "AFORO_COMPLETO",
                    "El evento ha completado su aforo");
            case "P0004" -> problema(HttpStatus.UNPROCESSABLE_ENTITY, "EDAD_FUERA_DE_TRAMO",
                    "La edad del menor no está dentro del tramo de la escuela");
            case "P0005" -> problema(HttpStatus.CONFLICT, "SIN_PLAZAS",
                    "No quedan plazas en la escuela");
            case "P0006" -> problema(HttpStatus.UNPROCESSABLE_ENTITY, "REPRESENTANTE_NO_VALIDO",
                    "Solo un usuario externo puede representar a una entidad con convenio");
            // Red de seguridad: con el candado por instalación no deberían darse, pero si otra vía
            // compite por las mismas filas, el usuario recibe un 409 y no un 500.
            case "40P01", "40001", "55P03" -> problema(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENTE",
                    "Otra persona estaba reservando a la vez. Inténtalo de nuevo");
            case "23514" -> problema(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INCOHERENTES",
                    "Los datos enviados no son coherentes");
            case "23505" -> mensajeSql.contains("uq_reserva_usuario_dia")
                    ? problema(HttpStatus.CONFLICT, "LIMITE_DIARIO",
                            "Ya tienes una reserva ese día: solo se permite una reserva por día")
                    : mensajeSql.contains("usuario_email_key")
                    ? problema(HttpStatus.CONFLICT, "EMAIL_EN_USO", "Ya existe una cuenta con ese email")
                    : mensajeSql.contains("instalacion_nombre_key")
                    ? problema(HttpStatus.CONFLICT, "INSTALACION_DUPLICADA", "Ya existe una instalación con ese nombre")
                    : mensajeSql.contains("entidad_convenio_cif_key")
                    ? problema(HttpStatus.CONFLICT, "ENTIDAD_DUPLICADA", "Ya existe una entidad con ese CIF")
                    : mensajeSql.contains("uq_escuela")
                    ? problema(HttpStatus.CONFLICT, "ESCUELA_DUPLICADA",
                            "Ya existe una escuela con ese nombre en esa temporada")
                    : problema(HttpStatus.CONFLICT, "DUPLICADO", "Ya existe un registro con esos datos");
            case "23503" -> problema(HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA",
                    "Se hace referencia a un elemento que no existe");
            default -> {
                log.error("Error SQL no contemplado (SQLSTATE {})", sqlState, ex);
                yield problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error interno del servidor");
            }
        };
    }

    /** Errores de @Valid: 400 con el detalle por campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los datos enviados");
        pd.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Resto de errores que Spring MVC resuelve por su cuenta (JSON mal formado, parámetro que
     * falta o con tipo incorrecto, ruta inexistente, método no permitido...): se les añade el
     * "codigo" para que TODAS las respuestas de error sigan el mismo contrato.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // Algunos errores (p. ej. parámetro obligatorio ausente) llegan aquí sin cuerpo todavía.
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail pd
                && (pd.getProperties() == null || !pd.getProperties().containsKey("codigo"))) {
            pd.setProperty("codigo", codigoPorEstado(statusCode));
            switch (statusCode.value()) {
                case 400 -> pd.setDetail("La petición no tiene un formato válido");
                case 404 -> pd.setDetail("La ruta solicitada no existe");
                case 405 -> pd.setDetail("Método HTTP no permitido en esta ruta");
                default -> { /* se mantiene el detalle de Spring */ }
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    static String codigoPorEstado(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "PETICION_INVALIDA";
            case 404 -> "RUTA_NO_ENCONTRADA";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "TIPO_NO_SOPORTADO";
            default -> "ERROR_HTTP_" + statusCode.value();
        };
    }

    /** Recorre la cadena de causas hasta encontrar la SQLException original de PostgreSQL. */
    static SQLException causaSql(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql;
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return null;
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("codigo", codigo);
        return pd;
    }
}
