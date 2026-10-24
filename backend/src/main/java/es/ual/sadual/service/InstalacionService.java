package es.ual.sadual.service;

import es.ual.sadual.dto.InstalacionDto;
import es.ual.sadual.dto.TipoInstalacionDto;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.repository.InstalacionRepository;
import es.ual.sadual.repository.TipoInstalacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F02 · Catálogo de instalaciones. */
@Service
public class InstalacionService {

    private final InstalacionRepository instalaciones;
    private final TipoInstalacionRepository tipos;

    public InstalacionService(InstalacionRepository instalaciones, TipoInstalacionRepository tipos) {
        this.instalaciones = instalaciones;
        this.tipos = tipos;
    }

    @Transactional(readOnly = true)
    public List<InstalacionDto> catalogo(Short idTipo) {
        List<Instalacion> lista = idTipo == null
                ? instalaciones.buscarActivas()
                : instalaciones.buscarActivasPorTipo(idTipo);
        return lista.stream().map(InstalacionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public InstalacionDto detalle(Integer id) {
        return InstalacionDto.from(activa(id));
    }

    @Transactional(readOnly = true)
    public List<TipoInstalacionDto> tipos() {
        return tipos.findAllByOrderByNombreAsc().stream().map(TipoInstalacionDto::from).toList();
    }

    /** Instalación activa o 404. Las desactivadas no se muestran ni se pueden reservar. */
    @Transactional(readOnly = true)
    public Instalacion activa(Integer id) {
        return instalaciones.findById(id)
                .filter(Instalacion::isActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("INSTALACION_NO_ENCONTRADA",
                        "La instalación no existe o no está disponible"));
    }
}
