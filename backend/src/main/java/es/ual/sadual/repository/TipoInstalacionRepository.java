package es.ual.sadual.repository;

import es.ual.sadual.entity.TipoInstalacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoInstalacionRepository extends JpaRepository<TipoInstalacion, Short> {

    List<TipoInstalacion> findAllByOrderByNombreAsc();
}
