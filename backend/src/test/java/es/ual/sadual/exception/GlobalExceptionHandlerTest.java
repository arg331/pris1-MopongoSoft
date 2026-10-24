package es.ual.sadual.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.jpa.JpaSystemException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** La traducción SQLSTATE → HTTP documentada en backend/README.md. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest(name = "SQLSTATE {0} -> {1} {2}")
    @CsvSource({
            "23P01, 409, FRANJA_OCUPADA",
            "P0001, 409, FRANJA_BLOQUEADA",
            "P0002, 422, FUERA_DE_HORARIO",
            "23514, 422, DATOS_INCOHERENTES",
            "23505, 409, DUPLICADO",
            "40P01, 409, CONFLICTO_CONCURRENTE",
            "40001, 409, CONFLICTO_CONCURRENTE",
            "XX000, 500, ERROR_INTERNO"
    })
    void traduce_el_sqlstate_aunque_venga_envuelto(String sqlState, int status, String codigo) {
        SQLException original = new SQLException("error de PostgreSQL", sqlState);
        RuntimeException envuelta = new DataIntegrityViolationException("capa Spring",
                new RuntimeException("capa Hibernate", original));

        ProblemDetail pd = handler.baseDeDatos(envuelta);

        assertThat(pd.getStatus()).isEqualTo(status);
        assertThat(pd.getProperties()).containsEntry("codigo", codigo);
    }

    @ParameterizedTest(name = "HTTP {0} -> {1}")
    @CsvSource({"400, PETICION_INVALIDA", "404, RUTA_NO_ENCONTRADA", "405, METODO_NO_PERMITIDO",
            "415, TIPO_NO_SOPORTADO", "406, ERROR_HTTP_406"})
    void los_errores_propios_de_Spring_MVC_tambien_llevan_codigo(int status, String codigo) {
        assertThat(GlobalExceptionHandler.codigoPorEstado(org.springframework.http.HttpStatusCode.valueOf(status)))
                .isEqualTo(codigo);
    }

    @org.junit.jupiter.api.Test
    void P1_el_indice_de_una_reserva_por_dia_se_traduce_a_LIMITE_DIARIO() {
        SQLException original = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uq_reserva_usuario_dia\"", "23505");
        ProblemDetail pd = handler.baseDeDatos(new DataIntegrityViolationException("x", original));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties()).containsEntry("codigo", "LIMITE_DIARIO");
    }

    @ParameterizedTest
    @CsvSource({"P0001, 409", "P0002, 422"})
    void tambien_traduce_errores_de_trigger_que_llegan_como_JpaSystemException(String sqlState, int status) {
        RuntimeException ex = new JpaSystemException(new RuntimeException(new SQLException("trigger", sqlState)));
        assertThat(handler.baseDeDatos(ex).getStatus()).isEqualTo(status);
    }
}
