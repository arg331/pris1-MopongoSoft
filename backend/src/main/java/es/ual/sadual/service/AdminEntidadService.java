package es.ual.sadual.service;

import es.ual.sadual.dto.EntidadAdminDto;
import es.ual.sadual.dto.EntidadRequest;
import es.ual.sadual.dto.SuspensionRequest;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Entidades con convenio (decisiones del cliente del 11/11/2026).
 * <ul>
 *   <li>P25 · Las da de alta la administración; una entidad puede tener varios representantes.</li>
 *   <li>P32 · Cada ausencia a una reserva institucional autorizada (reserva NO_PRESENTADO de la entidad)
 *       queda registrada. Con 3 o más durante la vigencia del convenio, la administración PUEDE suspender
 *       temporalmente las nuevas reservas institucionales de la entidad (no es automático).</li>
 * </ul>
 */
@Service
public class AdminEntidadService {

    private static final String CONSULTA = """
            SELECT e.id_entidad, e.nombre, e.cif, e.email_contacto, e.telefono, e.fecha_inicio, e.fecha_fin, e.activa,
                   e.suspendida_hasta, e.motivo_suspension,
                   (SELECT count(*) FROM usuario u WHERE u.id_entidad = e.id_entidad) AS representantes,
                   (SELECT count(*) FROM usuario u WHERE u.id_entidad = e.id_entidad AND NOT u.activo) AS pendientes,
                   (SELECT count(*) FROM reserva r
                     WHERE r.id_entidad = e.id_entidad AND r.estado = 'NO_PRESENTADO'
                       AND r.fecha >= e.fecha_inicio AND (e.fecha_fin IS NULL OR r.fecha <= e.fecha_fin)) AS ausencias
              FROM entidad_convenio e
            """;

    private final JdbcTemplate jdbc;
    private final int ausenciasParaSuspender;
    private final Clock reloj;

    public AdminEntidadService(JdbcTemplate jdbc,
                               @Value("${sadual.institucionales.ausencias-para-suspender:3}") int ausenciasParaSuspender,
                               Clock reloj) {
        this.jdbc = jdbc;
        this.ausenciasParaSuspender = ausenciasParaSuspender;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<EntidadAdminDto> todas() {
        return jdbc.query(CONSULTA + " ORDER BY e.nombre", (rs, n) -> fila(rs));
    }

    @Transactional
    public EntidadAdminDto crear(EntidadRequest p) {
        validarFechas(p);
        Integer id = jdbc.queryForObject("""
                INSERT INTO entidad_convenio (nombre, cif, email_contacto, telefono, fecha_inicio, fecha_fin, activa)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_entidad
                """, Integer.class, p.nombre().trim(), p.cif().trim().toUpperCase(Locale.ROOT), blancoANulo(p.emailContacto()),
                blancoANulo(p.telefono()), p.fechaInicio(), p.fechaFin(), p.activa());
        return una(id);
    }

    @Transactional
    public EntidadAdminDto editar(Integer id, EntidadRequest p) {
        una(id);
        validarFechas(p);
        jdbc.update("""
                UPDATE entidad_convenio SET nombre = ?, cif = ?, email_contacto = ?, telefono = ?, fecha_inicio = ?,
                       fecha_fin = ?, activa = ?
                 WHERE id_entidad = ?
                """, p.nombre().trim(), p.cif().trim().toUpperCase(Locale.ROOT), blancoANulo(p.emailContacto()),
                blancoANulo(p.telefono()), p.fechaInicio(), p.fechaFin(), p.activa(), id);
        return una(id);
    }

    /** P32 · Suspensión temporal decidida por la administración tras las ausencias de la entidad. */
    @Transactional
    public EntidadAdminDto suspender(Integer id, SuspensionRequest p) {
        EntidadAdminDto e = una(id);
        if (e.ausencias() < ausenciasParaSuspender) {
            throw new ConflictoException("AUSENCIAS_INSUFICIENTES", "La entidad tiene " + e.ausencias()
                    + " ausencia(s); se puede suspender a partir de " + ausenciasParaSuspender + " (P32)");
        }
        if (!p.hasta().isAfter(LocalDate.now(reloj))) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "La suspensión debe terminar en una fecha futura");
        }
        jdbc.update("UPDATE entidad_convenio SET suspendida_hasta = ?, motivo_suspension = ? WHERE id_entidad = ?",
                p.hasta(), p.motivo().trim(), id);
        return una(id);
    }

    @Transactional
    public EntidadAdminDto levantarSuspension(Integer id) {
        una(id);
        jdbc.update("UPDATE entidad_convenio SET suspendida_hasta = NULL, motivo_suspension = NULL WHERE id_entidad = ?", id);
        return una(id);
    }

    private EntidadAdminDto una(Integer id) {
        return jdbc.query(CONSULTA + " WHERE e.id_entidad = ?", (rs, n) -> fila(rs), id).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("ENTIDAD_NO_ENCONTRADA", "La entidad no existe"));
    }

    private static void validarFechas(EntidadRequest p) {
        if (p.fechaFin() != null && p.fechaFin().isBefore(p.fechaInicio())) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "El convenio no puede terminar antes de empezar");
        }
    }

    private EntidadAdminDto fila(java.sql.ResultSet rs) throws java.sql.SQLException {
        int ausencias = rs.getInt("ausencias");
        return new EntidadAdminDto(rs.getInt("id_entidad"), rs.getString("nombre"), rs.getString("cif"),
                rs.getString("email_contacto"), rs.getString("telefono"), rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class), rs.getBoolean("activa"), rs.getInt("representantes"),
                rs.getInt("pendientes"), ausencias, ausencias >= ausenciasParaSuspender,
                rs.getObject("suspendida_hasta", LocalDate.class), rs.getString("motivo_suspension"));
    }

    private static String blancoANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
