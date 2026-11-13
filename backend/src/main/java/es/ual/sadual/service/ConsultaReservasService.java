package es.ual.sadual.service;

import es.ual.sadual.dto.ReservaAdminDto;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.exception.ReglaNegocioException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * F10 · Consulta general de reservas (núcleo común obligatorio): filtros por fechas, instalación,
 * estado y texto (usuario o entidad). Por defecto, desde hoy y durante los próximos 30 días.
 */
@Service
public class ConsultaReservasService {

    static final int MAXIMO = 1000;

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public ConsultaReservasService(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ReservaAdminDto> buscar(LocalDate desde, LocalDate hasta, Integer idInstalacion, EstadoReserva estado,
                                        String texto) {
        LocalDate d = desde != null ? desde : LocalDate.now(reloj);
        LocalDate h = hasta != null ? hasta : d.plusDays(30);
        if (h.isBefore(d)) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "La fecha final no puede ser anterior a la inicial");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT id_reserva, fecha, hora_inicio, hora_fin, estado, id_instalacion, instalacion, tipo,
                       id_usuario, usuario, rol, entidad
                  FROM v_reservas_detalle
                 WHERE fecha BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(d, h));
        if (idInstalacion != null) {
            sql.append(" AND id_instalacion = ?");
            args.add(idInstalacion);
        }
        if (estado != null) {
            sql.append(" AND estado = ?");
            args.add(estado.name());
        }
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (usuario ILIKE ? OR coalesce(entidad, '') ILIKE ?)");
            args.add("%" + texto.trim() + "%");
            args.add("%" + texto.trim() + "%");
        }
        sql.append(" ORDER BY fecha, hora_inicio, instalacion LIMIT ").append(MAXIMO);
        return jdbc.query(sql.toString(), (rs, n) -> new ReservaAdminDto(rs.getInt("id_reserva"),
                rs.getObject("fecha", LocalDate.class), rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class), rs.getString("estado"), rs.getInt("id_instalacion"),
                rs.getString("instalacion"), rs.getString("tipo"), rs.getInt("id_usuario"), rs.getString("usuario"),
                rs.getString("rol"), rs.getString("entidad")), args.toArray());
    }
}
