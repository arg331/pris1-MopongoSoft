package es.ual.sadual.repository;

import es.ual.sadual.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Short> {

    Optional<Rol> findByCodigo(String codigo);
}
