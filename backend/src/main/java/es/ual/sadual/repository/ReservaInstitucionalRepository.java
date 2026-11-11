package es.ual.sadual.repository;

import es.ual.sadual.dto.ReservaInstitucionalDto;
import es.ual.sadual.entity.EstadoReserva;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** P21 · Consultas de lectura de reservas institucionales para el panel de administración. */
@Repository
public class ReservaInstitucionalRepository {

    private static final String CONSULTA = """
            SELECT r.id_reserva, r.id_instalacion, i.nombre AS instalacion, r.fecha, r.hora_inicio, r.hora_fin,
                   r.estado, r.id_entidad, e.nombre AS entidad,
                   u.nombre || ' ' || u.apellidos AS representante, u.email,
                   a.nombre || ' ' || a.apellidos AS autorizador,
                   r.fecha_creacion, r.fecha_resolucion, r.motivo_rechazo
              FROM reserva r
              JOIN instalacion i      ON i.id_instalacion = r.id_instalacion
              JOIN entidad_convenio e ON e.id_entidad     = r.id_entidad
              JOIN usuario u          ON u.id_usuario     = r.id_usuario
              LEFT JOIN usuario a     ON a.id_usuario     = r.id_autorizador
            """;

    private final JdbcTemplate jdbc;

    public ReservaInstitucionalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Pendientes primero y, dentro de cada estado, por fecha de uso. */
    public List<ReservaInstitucionalDto> buscar(EstadoReserva estado) {
        String orden = " ORDER BY (r.estado = 'PENDIENTE') DESC, r.fecha, r.hora_inicio";
        if (estado == null) {
            return jdbc.query(CONSULTA + orden, (rs, n) -> fila(rs));
        }
        return jdbc.query(CONSULTA + " WHERE r.estado = ?" + orden, (rs, n) -> fila(rs), estado.name());
    }

    private static ReservaInstitucionalDto fila(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReservaInstitucionalDto(
                rs.getInt("id_reserva"), rs.getInt("id_instalacion"), rs.getString("instalacion"),
                rs.getObject("fecha", LocalDate.class), rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class), EstadoReserva.valueOf(rs.getString("estado")),
                rs.getInt("id_entidad"), rs.getString("entidad"), rs.getString("representante"),
                rs.getString("email"), rs.getString("autorizador"),
                rs.getObject("fecha_creacion", LocalDateTime.class),
                rs.getObject("fecha_resolucion", LocalDateTime.class), rs.getString("motivo_rechazo"));
    }
}
