package es.ual.sadual.repository;

import es.ual.sadual.entity.Instalacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InstalacionRepository extends JpaRepository<Instalacion, Integer> {

    @Query("select i from Instalacion i join fetch i.tipo t where i.activa = true order by t.nombre, i.nombre")
    List<Instalacion> buscarActivas();

    @Query("select i from Instalacion i join fetch i.tipo t where i.activa = true and t.id = :idTipo order by i.nombre")
    List<Instalacion> buscarActivasPorTipo(@Param("idTipo") Short idTipo);
}
