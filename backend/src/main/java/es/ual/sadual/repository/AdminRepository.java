package es.ual.sadual.repository;

import es.ual.sadual.dto.HorarioDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Consultas y escrituras del panel de administración que no encajan en una entidad JPA:
 * horarios de apertura (F06), bloqueos (F08) y recuentos de reservas afectadas.
 */
@Repository
public class AdminRepository {

    private final JdbcTemplate jdbc;

    public AdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- horarios (F06)

    public List<HorarioDto> horario(Integer idInstalacion) {
        return jdbc.query("""
                SELECT dia_semana, hora_apertura, hora_cierre FROM horario_instalacion
                 WHERE id_instalacion = ? ORDER BY dia_semana, hora_apertura
                """, (rs, n) -> new HorarioDto(rs.getInt("dia_semana"), rs.getObject("hora_apertura", LocalTime.class),
                rs.getObject("hora_cierre", LocalTime.class)), idInstalacion);
    }

    /** Sustituye el horario semanal completo de la instalación. */
    public void reemplazarHorario(Integer idInstalacion, List<HorarioDto> horario) {
        jdbc.update("DELETE FROM horario_instalacion WHERE id_instalacion = ?", idInstalacion);
        for (HorarioDto h : horario) {
            jdbc.update("""
                    INSERT INTO horario_instalacion (id_instalacion, dia_semana, hora_apertura, hora_cierre)
                    VALUES (?, ?, ?, ?)
                    """, idInstalacion, h.diaSemana(), h.horaApertura(), h.horaCierre());
        }
    }

    /** Reservas activas (pendientes o confirmadas) que todavía no han empezado. */
    public int reservasFuturas(Integer idInstalacion, LocalDateTime ahora) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM reserva
                 WHERE id_instalacion = ? AND estado IN ('PENDIENTE','CONFIRMADA') AND fecha + hora_inicio > ?
                """, Integer.class, idInstalacion, ahora);
        return n == null ? 0 : n;
    }

    // ---------------------------------------------------------------- bloqueos (F08)

    public record BloqueoBd(Integer id, Integer idInstalacion, String instalacion, LocalDateTime inicio,
                            LocalDateTime fin, String motivo, String observaciones, String creador,
                            Integer idEvento, Integer idEscuela) {
    }

    public List<BloqueoBd> bloqueos(Integer idInstalacion, LocalDateTime desde, LocalDateTime hasta) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id_bloqueo, b.id_instalacion, i.nombre AS instalacion, b.inicio, b.fin, b.motivo,
                       b.observaciones, u.nombre || ' ' || u.apellidos AS creador, b.id_evento, b.id_escuela
                  FROM bloqueo b
                  JOIN instalacion i ON i.id_instalacion = b.id_instalacion
                  JOIN usuario u     ON u.id_usuario     = b.id_creador
                 WHERE b.fin > ? AND b.inicio < ?
                """);
        List<Object> args = new java.util.ArrayList<>(List.of(desde, hasta));
        if (idInstalacion != null) {
            sql.append(" AND b.id_instalacion = ?");
            args.add(idInstalacion);
        }
        sql.append(" ORDER BY b.inicio, i.nombre LIMIT 2000");
        return jdbc.query(sql.toString(), (rs, n) -> bloqueo(rs), args.toArray());
    }

    public java.util.Optional<BloqueoBd> bloqueo(Integer idBloqueo) {
        return jdbc.query("""
                SELECT b.id_bloqueo, b.id_instalacion, i.nombre AS instalacion, b.inicio, b.fin, b.motivo,
                       b.observaciones, u.nombre || ' ' || u.apellidos AS creador, b.id_evento, b.id_escuela
                  FROM bloqueo b
                  JOIN instalacion i ON i.id_instalacion = b.id_instalacion
                  JOIN usuario u     ON u.id_usuario     = b.id_creador
                 WHERE b.id_bloqueo = ?
                """, (rs, n) -> bloqueo(rs), idBloqueo).stream().findFirst();
    }

    public List<String> motivosSolapados(Integer idInstalacion, LocalDateTime inicio, LocalDateTime fin) {
        return jdbc.queryForList("""
                SELECT motivo FROM bloqueo
                 WHERE id_instalacion = ? AND tsrange(inicio, fin) && tsrange(?::timestamp, ?::timestamp)
                """, String.class, idInstalacion, inicio, fin);
    }

    public int reservasActivasSolapadas(Integer idInstalacion, LocalDateTime inicio, LocalDateTime fin) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM reserva
                 WHERE (?::int IS NULL OR id_instalacion = ?) AND estado IN ('PENDIENTE','CONFIRMADA')
                   AND tsrange(fecha + hora_inicio, fecha + hora_fin) && tsrange(?::timestamp, ?::timestamp)
                """, Integer.class, idInstalacion, idInstalacion, inicio, fin);
        return n == null ? 0 : n;
    }

    /** El trigger tg_bloqueo_cancela_reservas cancela las reservas que coinciden (P7). */
    public Integer insertarBloqueo(Integer idInstalacion, Integer idCreador, LocalDateTime inicio, LocalDateTime fin,
                                   String motivo, String observaciones) {
        return jdbc.queryForObject("""
                INSERT INTO bloqueo (id_instalacion, id_creador, inicio, fin, motivo, observaciones)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id_bloqueo
                """, Integer.class, idInstalacion, idCreador, inicio, fin, motivo, observaciones);
    }

    public void borrarBloqueo(Integer idBloqueo) {
        jdbc.update("DELETE FROM bloqueo WHERE id_bloqueo = ?", idBloqueo);
    }

    /** P11 · Cierre de todas las instalaciones activas (fn_cerrar_instalaciones). */
    public int cerrarInstalaciones(LocalDateTime inicio, LocalDateTime fin, String observaciones, Integer idCreador) {
        Integer n = jdbc.queryForObject("SELECT fn_cerrar_instalaciones(?::timestamp, ?::timestamp, ?, ?)",
                Integer.class, inicio, fin, observaciones, idCreador);
        return n == null ? 0 : n;
    }

    private static BloqueoBd bloqueo(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BloqueoBd(rs.getInt("id_bloqueo"), rs.getInt("id_instalacion"), rs.getString("instalacion"),
                rs.getObject("inicio", LocalDateTime.class), rs.getObject("fin", LocalDateTime.class),
                rs.getString("motivo"), rs.getString("observaciones"), rs.getString("creador"),
                (Integer) rs.getObject("id_evento"), (Integer) rs.getObject("id_escuela"));
    }
}
