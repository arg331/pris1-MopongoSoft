package es.ual.sadual.service;

import es.ual.sadual.dto.InformeDto;
import es.ual.sadual.dto.InformeDto.Cancelaciones;
import es.ual.sadual.dto.InformeDto.NoPresentados;
import es.ual.sadual.dto.InformeDto.OcupacionFranja;
import es.ual.sadual.dto.InformeDto.OcupacionInstalacion;
import es.ual.sadual.dto.InformeDto.PorInstalacion;
import es.ual.sadual.dto.InformeDto.PorUsuario;
import es.ual.sadual.dto.InformeDto.UsoPerfil;
import es.ual.sadual.exception.ReglaNegocioException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * P9 · Informes para el Servicio de Deportes (Incremento 3), validados el 25/10: ocupación por
 * instalación y franja, uso por perfil, cancelaciones y no presentados, con consulta semanal
 * (lunes a domingo) y mensual (mes natural).
 * <p>
 * Definiciones propuestas (❓P51): una reserva «ocupa» si está confirmada, finalizada o marcada como no
 * presentada (las pendientes y las canceladas no); el porcentaje se calcula sobre las horas en que la
 * instalación estaba abierta y no bloqueada; cancelaciones y no presentados se cuentan por la fecha de
 * uso de la reserva. Las reservas institucionales cuentan en la ocupación y aparecen como perfil
 * «INSTITUCIONAL» en el uso por perfil.
 */
@Service
public class InformeService {

    private static final String ESTADOS_OCUPAN = "('CONFIRMADA','FINALIZADA','NO_PRESENTADO')";

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public InformeService(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public InformeDto generar(String periodo, LocalDate fecha) {
        String tipo = periodo == null ? "SEMANAL" : periodo.trim().toUpperCase(Locale.ROOT);
        LocalDate referencia = fecha != null ? fecha : LocalDate.now(reloj);
        LocalDate desde;
        LocalDate hasta; // exclusivo
        switch (tipo) {
            case "SEMANAL" -> {
                desde = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                hasta = desde.plusWeeks(1);
            }
            case "MENSUAL" -> {
                desde = referencia.withDayOfMonth(1);
                hasta = desde.plusMonths(1);
            }
            default -> throw new ReglaNegocioException("PERIODO_NO_VALIDO", "El periodo debe ser SEMANAL o MENSUAL");
        }
        return new InformeDto(tipo, desde, hasta.minusDays(1), ocupacion(desde, hasta), franjas(desde, hasta),
                perfiles(desde, hasta), cancelaciones(desde, hasta), noPresentados(desde, hasta));
    }

    private List<OcupacionInstalacion> ocupacion(LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                WITH dias AS (SELECT d::date AS dia FROM generate_series(?::date, ?::date - 1, INTERVAL '1 day') d),
                abierto AS (
                    SELECT h.id_instalacion, tsrange(dia + h.hora_apertura, dia + h.hora_cierre) AS r
                      FROM horario_instalacion h JOIN dias ON EXTRACT(ISODOW FROM dia) = h.dia_semana)
                SELECT i.id_instalacion, i.nombre, t.nombre AS tipo,
                       coalesce((SELECT sum(EXTRACT(EPOCH FROM upper(a.r) - lower(a.r))) FROM abierto a
                                  WHERE a.id_instalacion = i.id_instalacion), 0) / 3600 AS h_apertura,
                       coalesce((SELECT sum(EXTRACT(EPOCH FROM upper(a.r * tsrange(b.inicio, b.fin))
                                                              - lower(a.r * tsrange(b.inicio, b.fin))))
                                   FROM abierto a JOIN bloqueo b
                                     ON b.id_instalacion = a.id_instalacion AND tsrange(b.inicio, b.fin) && a.r
                                  WHERE a.id_instalacion = i.id_instalacion), 0) / 3600 AS h_bloqueadas,
                       coalesce((SELECT sum(EXTRACT(EPOCH FROM r.hora_fin - r.hora_inicio)) FROM reserva r
                                  WHERE r.id_instalacion = i.id_instalacion AND r.fecha >= ? AND r.fecha < ?
                                    AND r.estado IN """ + ESTADOS_OCUPAN + "), 0) / 3600 AS h_reservadas" + """

                  FROM instalacion i JOIN tipo_instalacion t ON t.id_tipo = i.id_tipo
                 WHERE i.activa
                 ORDER BY t.nombre, i.nombre
                """, (rs, n) -> {
            BigDecimal apertura = horas(rs.getBigDecimal("h_apertura"));
            BigDecimal bloqueadas = horas(rs.getBigDecimal("h_bloqueadas"));
            BigDecimal reservadas = horas(rs.getBigDecimal("h_reservadas"));
            BigDecimal disponibles = apertura.subtract(bloqueadas);
            BigDecimal porcentaje = disponibles.signum() <= 0 ? BigDecimal.ZERO
                    : reservadas.multiply(BigDecimal.valueOf(100)).divide(disponibles, 1, RoundingMode.HALF_UP);
            return new OcupacionInstalacion(rs.getInt("id_instalacion"), rs.getString("nombre"), rs.getString("tipo"),
                    apertura, bloqueadas, reservadas, porcentaje);
        }, desde, hasta, desde, hasta);
    }

    private List<OcupacionFranja> franjas(LocalDate desde, LocalDate hasta) {
        return jdbc.query("SELECT EXTRACT(HOUR FROM hora_inicio)::int AS hora, count(*) AS n FROM reserva "
                        + "WHERE fecha >= ? AND fecha < ? AND estado IN " + ESTADOS_OCUPAN + " GROUP BY 1 ORDER BY 1",
                (rs, n) -> new OcupacionFranja(rs.getInt("hora"), rs.getInt("n")), desde, hasta);
    }

    private List<UsoPerfil> perfiles(LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                SELECT CASE WHEN r.id_entidad IS NULL THEN ro.codigo ELSE 'INSTITUCIONAL' END AS perfil,
                       count(*) AS reservas, count(DISTINCT r.id_usuario) AS usuarios,
                       sum(EXTRACT(EPOCH FROM r.hora_fin - r.hora_inicio)) / 3600 AS horas
                  FROM reserva r JOIN usuario u ON u.id_usuario = r.id_usuario JOIN rol ro ON ro.id_rol = u.id_rol
                 WHERE r.fecha >= ? AND r.fecha < ? AND r.estado IN """ + ESTADOS_OCUPAN + """

                 GROUP BY 1 ORDER BY 2 DESC, 1
                """, (rs, n) -> new UsoPerfil(rs.getString("perfil"), rs.getInt("reservas"), rs.getInt("usuarios"),
                horas(rs.getBigDecimal("horas"))), desde, hasta);
    }

    private Cancelaciones cancelaciones(LocalDate desde, LocalDate hasta) {
        int canceladas = contar("CANCELADA", desde, hasta);
        int rechazadas = contar("RECHAZADA", desde, hasta);
        List<PorInstalacion> porInstalacion = jdbc.query("""
                SELECT i.nombre, count(*) AS n FROM reserva r JOIN instalacion i ON i.id_instalacion = r.id_instalacion
                 WHERE r.fecha >= ? AND r.fecha < ? AND r.estado = 'CANCELADA'
                 GROUP BY i.nombre ORDER BY 2 DESC, 1
                """, (rs, n) -> new PorInstalacion(rs.getString("nombre"), rs.getInt("n")), desde, hasta);
        return new Cancelaciones(canceladas, rechazadas, porInstalacion);
    }

    private NoPresentados noPresentados(LocalDate desde, LocalDate hasta) {
        List<PorUsuario> porUsuario = jdbc.query("""
                SELECT u.id_usuario, u.nombre || ' ' || u.apellidos AS usuario, ro.codigo, count(*) AS n
                  FROM reserva r JOIN usuario u ON u.id_usuario = r.id_usuario JOIN rol ro ON ro.id_rol = u.id_rol
                 WHERE r.fecha >= ? AND r.fecha < ? AND r.estado = 'NO_PRESENTADO'
                 GROUP BY u.id_usuario, usuario, ro.codigo ORDER BY n DESC, usuario
                """, (rs, n) -> new PorUsuario(rs.getInt("id_usuario"), rs.getString("usuario"), rs.getString("codigo"),
                rs.getInt("n")), desde, hasta);
        return new NoPresentados(porUsuario.stream().mapToInt(PorUsuario::veces).sum(), porUsuario);
    }

    private int contar(String estado, LocalDate desde, LocalDate hasta) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM reserva WHERE fecha >= ? AND fecha < ? AND estado = ?",
                Integer.class, desde, hasta, estado);
        return n == null ? 0 : n;
    }

    private static BigDecimal horas(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(1) : valor.setScale(1, RoundingMode.HALF_UP);
    }
}
