package es.ual.sadual.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Acceso a la lógica de disponibilidad que vive en la base de datos.
 * Reutiliza fn_franjas_disponibles (01_schema.sql) para que el cálculo de franjas
 * sea idéntico al que valida el trigger y la restricción EXCLUDE.
 */
@Repository
public class FranjaRepository {

    public record FranjaBd(LocalTime horaInicio, LocalTime horaFin, boolean disponible) {
    }

    public record BloqueoBd(LocalDateTime inicio, LocalDateTime fin, String motivo) {
    }

    /** Primera clave del candado consultivo de reservas (la segunda es el id de la instalación). */
    static final int CLAVE_CANDADO_RESERVAS = 7001;

    private final JdbcTemplate jdbc;

    public FranjaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FranjaBd> franjas(Integer idInstalacion, LocalDate fecha) {
        return jdbc.query(
                "SELECT hora_inicio, hora_fin, disponible FROM fn_franjas_disponibles(?, ?)",
                (rs, n) -> new FranjaBd(
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getObject("hora_fin", LocalTime.class),
                        rs.getBoolean("disponible")),
                idInstalacion, fecha);
    }

    /**
     * Pone en fila las reservas de una misma instalación hasta el final de la transacción.
     * <p>
     * Sin esto, PostgreSQL puede abortar con un deadlock (40P01) a algunas de las transacciones
     * que insertan a la vez filas en conflicto bajo la restricción EXCLUDE: la regla se cumple,
     * pero el usuario recibiría un error en vez de "franja ocupada". Con el candado, cada petición
     * espera a que termine la anterior y luego ve el estado ya confirmado. Instalaciones distintas
     * no se esperan entre sí. Exige una transacción activa (se libera en el commit o rollback).
     */
    public void bloquearInstalacion(Integer idInstalacion) {
        jdbc.query("SELECT pg_advisory_xact_lock(?, ?)", rs -> null, CLAVE_CANDADO_RESERVAS, idInstalacion);
    }

    /** Bloqueos que tocan el día indicado (para distinguir "bloqueada" de "reservada"). */
    public List<BloqueoBd> bloqueosDelDia(Integer idInstalacion, LocalDate fecha) {
        return jdbc.query(
                "SELECT inicio, fin, motivo FROM bloqueo WHERE id_instalacion = ? AND inicio < ? AND fin > ?",
                (rs, n) -> new BloqueoBd(
                        rs.getObject("inicio", LocalDateTime.class),
                        rs.getObject("fin", LocalDateTime.class),
                        rs.getString("motivo")),
                idInstalacion, fecha.plusDays(1).atStartOfDay(), fecha.atStartOfDay());
    }
}
