package es.ual.sadual.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Entidades con convenio (P17, P21). Solo lectura: su alta la hace la administración. */
@Repository
public class EntidadRepository {

    public record EntidadBd(Integer id, String nombre, String cif, boolean activa, LocalDate fechaInicio,
                            LocalDate fechaFin, LocalDate suspendidaHasta) {

        /** P32: nuevas reservas institucionales suspendidas temporalmente por la administración. */
        public boolean suspendidaEn(LocalDate dia) {
            return suspendidaHasta != null && !dia.isAfter(suspendidaHasta);
        }

        /** Convenio activo y vigente en la fecha indicada. */
        public boolean vigenteEn(LocalDate dia) {
            return activa && !dia.isBefore(fechaInicio) && (fechaFin == null || !dia.isAfter(fechaFin));
        }
    }

    private static final String COLUMNAS =
            "SELECT id_entidad, nombre, cif, activa, fecha_inicio, fecha_fin, suspendida_hasta FROM entidad_convenio";

    private final JdbcTemplate jdbc;

    public EntidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EntidadBd> porCif(String cif) {
        return jdbc.query(COLUMNAS + " WHERE upper(cif) = upper(?)", (rs, n) -> fila(rs), cif)
                .stream().findFirst();
    }

    public Optional<EntidadBd> porId(Integer id) {
        return jdbc.query(COLUMNAS + " WHERE id_entidad = ?", (rs, n) -> fila(rs), id)
                .stream().findFirst();
    }

    private static EntidadBd fila(ResultSet rs) throws SQLException {
        return new EntidadBd(rs.getInt("id_entidad"), rs.getString("nombre"), rs.getString("cif"),
                rs.getBoolean("activa"), rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class), rs.getObject("suspendida_hasta", LocalDate.class));
    }
}
