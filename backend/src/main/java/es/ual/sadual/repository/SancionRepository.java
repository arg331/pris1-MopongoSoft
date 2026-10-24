package es.ual.sadual.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/** P3: las sanciones las crea el trigger tg_sancionar_no_presentado; aquí solo se consultan. */
@Repository
public class SancionRepository {

    private final JdbcTemplate jdbc;

    public SancionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Último día de la sanción vigente en la fecha indicada, si la hay. */
    public Optional<LocalDate> vigenteEn(Integer idUsuario, LocalDate dia) {
        LocalDate fin = jdbc.queryForObject(
                "SELECT max(fecha_fin) FROM sancion WHERE id_usuario = ? AND ? BETWEEN fecha_inicio AND fecha_fin",
                LocalDate.class, idUsuario, dia);
        return Optional.ofNullable(fin);
    }
}
