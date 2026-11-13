package es.ual.sadual.repository;

import es.ual.sadual.dto.EscuelaDto;
import es.ual.sadual.dto.EscuelaRequest;
import es.ual.sadual.dto.InscritoEscuelaDto;
import es.ual.sadual.dto.MenorDto;
import es.ual.sadual.dto.SesionEscuelaDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * P19, P20, P22 · Escuelas infantil-juveniles, menores y sus inscripciones.
 * La edad y las plazas las comprueba el trigger tg_validar_inscripcion_escuela (P0004, P0005)
 * y los bloqueos de la temporada los genera fn_generar_bloqueos_escuela.
 */
@Repository
public class EscuelaRepository {

    public record MenorBd(Integer id, Integer idTutor) {
    }

    private static final String SELECT_ESCUELA = """
            SELECT e.id_escuela, e.nombre, e.temporada, e.fecha_inicio, e.fecha_fin, e.id_instalacion,
                   i.nombre AS instalacion, e.edad_min, e.edad_max, e.plazas, e.dias_horario, e.estado,
                   (SELECT count(*) FROM inscripcion_escuela ie
                     WHERE ie.id_escuela = e.id_escuela AND ie.estado = 'ACTIVA') AS inscritos
              FROM escuela e JOIN instalacion i ON i.id_instalacion = e.id_instalacion
            """;

    private final JdbcTemplate jdbc;

    public EscuelaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- escuelas

    public Integer insertar(EscuelaRequest p, String diasHorario) {
        GeneratedKeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO escuela (id_instalacion, nombre, temporada, fecha_inicio, fecha_fin,
                                         edad_min, edad_max, plazas, dias_horario)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id_escuela"});
            ps.setInt(1, p.idInstalacion());
            ps.setString(2, p.nombre().trim());
            ps.setString(3, p.temporada());
            ps.setDate(4, Date.valueOf(p.fechaInicio()));
            ps.setDate(5, Date.valueOf(p.fechaFin()));
            ps.setInt(6, p.edadMin());
            ps.setInt(7, p.edadMax());
            ps.setInt(8, p.plazas());
            ps.setString(9, diasHorario);
            return ps;
        }, clave);
        Integer id = Objects.requireNonNull(clave.getKey()).intValue();
        for (SesionEscuelaDto s : p.horario()) {
            jdbc.update("INSERT INTO horario_escuela (id_escuela, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)",
                    id, s.diaSemana(), s.horaInicio(), s.horaFin());
        }
        return id;
    }

    /** Reservas activas (confirmadas, P41, y pendientes, ❓P55) que coinciden con alguna sesión de la escuela. */
    public int reservasAfectadas(Integer idEscuela) {
        Integer n = jdbc.queryForObject("""
                SELECT count(DISTINCT r.id_reserva)
                  FROM escuela e
                  JOIN horario_escuela h ON h.id_escuela = e.id_escuela
                  JOIN reserva r ON r.id_instalacion = e.id_instalacion
                 WHERE e.id_escuela = ?
                   AND r.estado IN ('CONFIRMADA', 'PENDIENTE')
                   AND r.fecha BETWEEN e.fecha_inicio AND e.fecha_fin
                   AND EXTRACT(ISODOW FROM r.fecha) = h.dia_semana
                   AND r.hora_inicio < h.hora_fin AND r.hora_fin > h.hora_inicio
                """, Integer.class, idEscuela);
        return n == null ? 0 : n;
    }

    public int generarBloqueos(Integer idEscuela, Integer idCreador) {
        Integer n = jdbc.queryForObject("SELECT fn_generar_bloqueos_escuela(?, ?)", Integer.class, idEscuela, idCreador);
        return n == null ? 0 : n;
    }

    public Optional<EscuelaDto> buscar(Integer idEscuela) {
        return jdbc.query(SELECT_ESCUELA + " WHERE e.id_escuela = ?", (rs, n) -> escuela(rs), idEscuela)
                .stream().findFirst();
    }

    /** Escuelas cuya temporada no ha terminado. */
    public List<EscuelaDto> vigentes(LocalDate hoy) {
        return jdbc.query(SELECT_ESCUELA + " WHERE e.fecha_fin >= ? AND e.estado = 'ACTIVA' ORDER BY e.nombre",
                (rs, n) -> escuela(rs), hoy);
    }

    public List<EscuelaDto> todas() {
        return jdbc.query(SELECT_ESCUELA + " ORDER BY e.fecha_inicio DESC, e.nombre", (rs, n) -> escuela(rs));
    }

    public List<InscritoEscuelaDto> inscritos(Integer idEscuela) {
        return jdbc.query("""
                SELECT m.id_menor, m.nombre || ' ' || m.apellidos AS menor, m.fecha_nacimiento,
                       t.nombre || ' ' || t.apellidos AS tutor, t.email, m.telefono_tutor AS telefono,
                       ie.fecha_inscripcion, ie.estado
                  FROM inscripcion_escuela ie
                  JOIN menor m   ON m.id_menor   = ie.id_menor
                  JOIN usuario t ON t.id_usuario = m.id_tutor
                 WHERE ie.id_escuela = ?
                 ORDER BY ie.estado, m.apellidos, m.nombre
                """, (rs, n) -> new InscritoEscuelaDto(rs.getInt("id_menor"), rs.getString("menor"),
                rs.getObject("fecha_nacimiento", LocalDate.class), rs.getString("tutor"), rs.getString("email"),
                rs.getString("telefono"), rs.getObject("fecha_inscripcion", LocalDateTime.class),
                rs.getString("estado")), idEscuela);
    }

    // ---------------------------------------------------------------- menores e inscripciones

    public Integer insertarMenor(Integer idTutor, String nombre, String apellidos, LocalDate nacimiento,
                                 LocalDateTime consentimiento, String telefonoTutor, String emergenciaNombre,
                                 String emergenciaTelefono) {
        GeneratedKeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO menor (id_tutor, nombre, apellidos, fecha_nacimiento, consentimiento_rgpd,
                                       fecha_consentimiento, telefono_tutor, contacto_emergencia_nombre,
                                       contacto_emergencia_telefono)
                    VALUES (?, ?, ?, ?, TRUE, ?, ?, ?, ?)
                    """, new String[] {"id_menor"});
            ps.setInt(1, idTutor);
            ps.setString(2, nombre);
            ps.setString(3, apellidos);
            ps.setDate(4, Date.valueOf(nacimiento));
            ps.setTimestamp(5, Timestamp.valueOf(consentimiento));
            ps.setString(6, telefonoTutor);
            ps.setString(7, emergenciaNombre);
            ps.setString(8, emergenciaTelefono);
            return ps;
        }, clave);
        return Objects.requireNonNull(clave.getKey()).intValue();
    }

    public Optional<MenorBd> menor(Integer idMenor) {
        return jdbc.query("SELECT id_menor, id_tutor FROM menor WHERE id_menor = ?",
                (rs, n) -> new MenorBd(rs.getInt("id_menor"), rs.getInt("id_tutor")), idMenor).stream().findFirst();
    }

    public List<MenorDto> menoresDelTutor(Integer idTutor) {
        List<MenorDto> menores = jdbc.query("""
                SELECT id_menor, nombre, apellidos, fecha_nacimiento, fecha_consentimiento, telefono_tutor,
                       contacto_emergencia_nombre, contacto_emergencia_telefono
                  FROM menor WHERE id_tutor = ? ORDER BY nombre
                """, (rs, n) -> new MenorDto(rs.getInt("id_menor"), rs.getString("nombre"), rs.getString("apellidos"),
                rs.getObject("fecha_nacimiento", LocalDate.class),
                rs.getObject("fecha_consentimiento", LocalDateTime.class), rs.getString("telefono_tutor"),
                rs.getString("contacto_emergencia_nombre"), rs.getString("contacto_emergencia_telefono"), List.of()),
                idTutor);
        return menores.stream()
                .map(m -> new MenorDto(m.id(), m.nombre(), m.apellidos(), m.fechaNacimiento(), m.fechaConsentimiento(),
                        m.telefonoTutor(), m.contactoEmergenciaNombre(), m.contactoEmergenciaTelefono(),
                        inscripcionesDe(m.id())))
                .toList();
    }

    private List<MenorDto.InscripcionMenorDto> inscripcionesDe(Integer idMenor) {
        return jdbc.query("""
                SELECT e.id_escuela, e.nombre, e.temporada, ie.fecha_inscripcion, ie.estado
                  FROM inscripcion_escuela ie JOIN escuela e ON e.id_escuela = ie.id_escuela
                 WHERE ie.id_menor = ? ORDER BY ie.fecha_inscripcion DESC
                """, (rs, n) -> new MenorDto.InscripcionMenorDto(rs.getInt("id_escuela"), rs.getString("nombre"),
                rs.getString("temporada"), rs.getObject("fecha_inscripcion", LocalDateTime.class),
                rs.getString("estado")), idMenor);
    }

    /**
     * Inscribe o reactiva una baja. P20: la edad se calcula con fecha_inscripcion (reloj de negocio).
     * Devuelve 0 si el menor ya estaba inscrito.
     */
    public int inscribir(Integer idEscuela, Integer idMenor, LocalDateTime ahora) {
        return jdbc.update("""
                INSERT INTO inscripcion_escuela (id_escuela, id_menor, fecha_inscripcion, estado)
                VALUES (?, ?, ?, 'ACTIVA')
                ON CONFLICT (id_escuela, id_menor) DO UPDATE
                   SET estado = 'ACTIVA', fecha_inscripcion = EXCLUDED.fecha_inscripcion
                 WHERE inscripcion_escuela.estado = 'BAJA'
                """, idEscuela, idMenor, ahora);
    }

    /** ❓P56 · Cancela la escuela: libera sus sesiones futuras y da de baja a los inscritos. Devuelve las bajas. */
    public int cancelar(Integer idEscuela, LocalDateTime ahora) {
        jdbc.update("DELETE FROM bloqueo WHERE id_escuela = ? AND inicio >= ?", idEscuela, ahora);
        int bajas = jdbc.update("UPDATE inscripcion_escuela SET estado = 'BAJA' WHERE id_escuela = ? AND estado = 'ACTIVA'",
                idEscuela);
        jdbc.update("UPDATE escuela SET estado = 'CANCELADA' WHERE id_escuela = ?", idEscuela);
        return bajas;
    }

    /** ❓P52 · El tutor corrige los datos de contacto del menor. */
    public void actualizarContacto(Integer idMenor, String telefonoTutor, String emergenciaNombre,
                                   String emergenciaTelefono) {
        jdbc.update("""
                UPDATE menor SET telefono_tutor = ?, contacto_emergencia_nombre = ?, contacto_emergencia_telefono = ?
                 WHERE id_menor = ?
                """, telefonoTutor, emergenciaNombre, emergenciaTelefono, idMenor);
    }

    /** P45 · Al retirar el consentimiento se eliminan los datos del menor y, en cascada, sus inscripciones. */
    public void eliminarMenor(Integer idMenor) {
        jdbc.update("DELETE FROM menor WHERE id_menor = ?", idMenor);
    }

    public int darDeBaja(Integer idEscuela, Integer idMenor) {
        return jdbc.update("""
                UPDATE inscripcion_escuela SET estado = 'BAJA'
                 WHERE id_escuela = ? AND id_menor = ? AND estado = 'ACTIVA'
                """, idEscuela, idMenor);
    }

    // ---------------------------------------------------------------- filas

    private EscuelaDto escuela(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_escuela");
        int plazas = rs.getInt("plazas");
        int inscritos = rs.getInt("inscritos");
        List<SesionEscuelaDto> horario = jdbc.query("""
                SELECT dia_semana, hora_inicio, hora_fin FROM horario_escuela
                 WHERE id_escuela = ? ORDER BY dia_semana, hora_inicio
                """, (h, n) -> new SesionEscuelaDto(h.getInt("dia_semana"), h.getObject("hora_inicio", LocalTime.class),
                h.getObject("hora_fin", LocalTime.class)), id);
        return new EscuelaDto(id, rs.getString("nombre"), rs.getString("temporada"),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                rs.getInt("id_instalacion"), rs.getString("instalacion"), rs.getInt("edad_min"), rs.getInt("edad_max"),
                plazas, inscritos, Math.max(0, plazas - inscritos), rs.getString("dias_horario"), horario,
                rs.getString("estado"));
    }
}
