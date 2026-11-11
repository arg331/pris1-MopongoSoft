package es.ual.sadual.repository;

import es.ual.sadual.dto.InscritoEventoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * P18 · Eventos masivos. Un evento ocupa sus instalaciones mediante bloqueos EVENTO (uno por
 * instalación); el aforo lo controla el trigger tg_aforo_evento (P0003).
 */
@Repository
public class EventoRepository {

    public record EventoBd(Integer id, String nombre, String descripcion, LocalDateTime inicio, LocalDateTime fin,
                           int aforoMaximo, String estado, int inscritos, String publico) {
    }

    private static final String SELECT = """
            SELECT e.id_evento, e.nombre, e.descripcion, e.inicio, e.fin, e.aforo_maximo, e.estado, e.publico,
                   (SELECT count(*) FROM inscripcion_evento ie
                     WHERE ie.id_evento = e.id_evento AND ie.estado = 'ACTIVA') AS inscritos
              FROM evento e
            """;

    private final JdbcTemplate jdbc;

    public EventoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Integer insertar(Integer idOrganizador, String nombre, String descripcion, LocalDateTime inicio,
                            LocalDateTime fin, int aforoMaximo, String publico) {
        GeneratedKeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO evento (id_organizador, nombre, descripcion, inicio, fin, aforo_maximo, estado, publico)
                    VALUES (?, ?, ?, ?, ?, ?, 'PLANIFICADO', ?)
                    """, new String[] {"id_evento"});
            ps.setInt(1, idOrganizador);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);
            ps.setTimestamp(4, Timestamp.valueOf(inicio));
            ps.setTimestamp(5, Timestamp.valueOf(fin));
            ps.setInt(6, aforoMaximo);
            ps.setString(7, publico);
            return ps;
        }, clave);
        return Objects.requireNonNull(clave.getKey()).intValue();
    }

    /** El trigger tg_bloqueo_cancela_reservas cancela las reservas que coincidan (P7). */
    public void bloquearInstalacion(Integer idEvento, Integer idInstalacion, Integer idCreador,
                                    LocalDateTime inicio, LocalDateTime fin, String nombre) {
        jdbc.update("""
                INSERT INTO bloqueo (id_instalacion, id_creador, id_evento, inicio, fin, motivo, observaciones)
                VALUES (?, ?, ?, ?, ?, 'EVENTO', left(?, 255))
                """, idInstalacion, idCreador, idEvento, inicio, fin, "Evento: " + nombre);
    }

    /** Motivos de los bloqueos que ya ocupan la instalación en ese periodo. */
    public List<String> bloqueosSolapados(Integer idInstalacion, LocalDateTime inicio, LocalDateTime fin) {
        return jdbc.queryForList("""
                SELECT motivo FROM bloqueo
                 WHERE id_instalacion = ? AND tsrange(inicio, fin) && tsrange(?::timestamp, ?::timestamp)
                """, String.class, idInstalacion, inicio, fin);
    }

    public int reservasActivasSolapadas(Integer idInstalacion, LocalDateTime inicio, LocalDateTime fin) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM reserva
                 WHERE id_instalacion = ? AND estado IN ('PENDIENTE','CONFIRMADA')
                   AND tsrange(fecha + hora_inicio, fecha + hora_fin) && tsrange(?::timestamp, ?::timestamp)
                """, Integer.class, idInstalacion, inicio, fin);
        return n == null ? 0 : n;
    }

    public Optional<EventoBd> buscar(Integer idEvento) {
        return jdbc.query(SELECT + " WHERE e.id_evento = ?", (rs, n) -> fila(rs), idEvento).stream().findFirst();
    }

    /** Bloquea la fila del evento hasta el final de la transacción (cambios de estado e inscripciones). */
    public Optional<EventoBd> buscarParaActualizar(Integer idEvento) {
        jdbc.query("SELECT 1 FROM evento WHERE id_evento = ? FOR UPDATE", rs -> null, idEvento);
        return buscar(idEvento);
    }

    public List<EventoBd> todos() {
        return jdbc.query(SELECT + " ORDER BY e.inicio DESC", (rs, n) -> fila(rs));
    }

    public List<EventoBd> publicadosDesde(LocalDateTime ahora) {
        return jdbc.query(SELECT + " WHERE e.estado = 'PUBLICADO' AND e.fin > ? ORDER BY e.inicio",
                (rs, n) -> fila(rs), ahora);
    }

    public List<EventoBd> delUsuario(Integer idUsuario) {
        return jdbc.query(SELECT + """
                 WHERE EXISTS (SELECT 1 FROM inscripcion_evento ie
                                WHERE ie.id_evento = e.id_evento AND ie.id_usuario = ? AND ie.estado = 'ACTIVA')
                 ORDER BY e.inicio DESC
                """, (rs, n) -> fila(rs), idUsuario);
    }

    public List<String> instalaciones(Integer idEvento) {
        return jdbc.queryForList("""
                SELECT DISTINCT i.nombre FROM bloqueo b JOIN instalacion i ON i.id_instalacion = b.id_instalacion
                 WHERE b.id_evento = ? ORDER BY i.nombre
                """, String.class, idEvento);
    }

    public void cambiarEstado(Integer idEvento, String estado) {
        jdbc.update("UPDATE evento SET estado = ? WHERE id_evento = ?", estado, idEvento);
    }

    /** P18: al cancelar el evento se anulan sus inscripciones (se conservan como histórico). */
    public int anularInscripciones(Integer idEvento) {
        return jdbc.update("UPDATE inscripcion_evento SET estado = 'ANULADA' WHERE id_evento = ? AND estado = 'ACTIVA'",
                idEvento);
    }

    /** Libera las instalaciones del evento cancelado. */
    public int borrarBloqueos(Integer idEvento) {
        return jdbc.update("DELETE FROM bloqueo WHERE id_evento = ?", idEvento);
    }

    public boolean inscritoActivo(Integer idEvento, Integer idUsuario) {
        Boolean si = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM inscripcion_evento
                                WHERE id_evento = ? AND id_usuario = ? AND estado = 'ACTIVA')
                """, Boolean.class, idEvento, idUsuario);
        return Boolean.TRUE.equals(si);
    }

    /**
     * Inscribe o reactiva una inscripción anulada. El aforo lo comprueba el trigger (P0003).
     * Devuelve 0 si el usuario ya estaba inscrito.
     */
    public int inscribir(Integer idEvento, Integer idUsuario, LocalDateTime ahora) {
        return jdbc.update("""
                INSERT INTO inscripcion_evento (id_evento, id_usuario, fecha_inscripcion, estado)
                VALUES (?, ?, ?, 'ACTIVA')
                ON CONFLICT (id_evento, id_usuario) DO UPDATE
                   SET estado = 'ACTIVA', fecha_inscripcion = EXCLUDED.fecha_inscripcion
                 WHERE inscripcion_evento.estado = 'ANULADA'
                """, idEvento, idUsuario, ahora);
    }

    public int anularInscripcion(Integer idEvento, Integer idUsuario) {
        return jdbc.update("""
                UPDATE inscripcion_evento SET estado = 'ANULADA'
                 WHERE id_evento = ? AND id_usuario = ? AND estado = 'ACTIVA'
                """, idEvento, idUsuario);
    }

    public List<InscritoEventoDto> inscritos(Integer idEvento) {
        return jdbc.query("""
                SELECT u.id_usuario, u.nombre || ' ' || u.apellidos AS nombre, u.email, r.codigo,
                       ie.fecha_inscripcion, ie.estado
                  FROM inscripcion_evento ie
                  JOIN usuario u ON u.id_usuario = ie.id_usuario
                  JOIN rol r     ON r.id_rol     = u.id_rol
                 WHERE ie.id_evento = ?
                 ORDER BY ie.estado, ie.fecha_inscripcion
                """, (rs, n) -> new InscritoEventoDto(rs.getInt("id_usuario"), rs.getString("nombre"),
                rs.getString("email"), rs.getString("codigo"),
                rs.getObject("fecha_inscripcion", LocalDateTime.class), rs.getString("estado")), idEvento);
    }

    private static EventoBd fila(ResultSet rs) throws SQLException {
        return new EventoBd(rs.getInt("id_evento"), rs.getString("nombre"), rs.getString("descripcion"),
                rs.getObject("inicio", LocalDateTime.class), rs.getObject("fin", LocalDateTime.class),
                rs.getInt("aforo_maximo"), rs.getString("estado"), rs.getInt("inscritos"), rs.getString("publico"));
    }
}
