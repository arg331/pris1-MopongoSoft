package es.ual.sadual.repository;

import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query("""
           select r from Reserva r
             join fetch r.instalacion i
             join fetch i.tipo
            where r.usuario.id = :idUsuario
            order by r.fecha desc, r.horaInicio desc
           """)
    List<Reserva> buscarDelUsuario(@Param("idUsuario") Integer idUsuario);

    /** P1: ¿tiene ya una reserva individual activa ese día? */
    boolean existsByUsuarioIdAndFechaAndEstadoInAndIdEntidadIsNull(Integer idUsuario, LocalDate fecha,
                                                                  Collection<EstadoReserva> estados);
}
