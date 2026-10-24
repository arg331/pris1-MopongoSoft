package es.ual.sadual.dto;

import es.ual.sadual.entity.TipoInstalacion;

public record TipoInstalacionDto(Short id, String nombre, String descripcion) {

    public static TipoInstalacionDto from(TipoInstalacion t) {
        return new TipoInstalacionDto(t.getId(), t.getNombre(), t.getDescripcion());
    }
}
