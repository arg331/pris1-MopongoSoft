package es.ual.sadual.dto;

import es.ual.sadual.entity.Instalacion;

public record InstalacionDto(Integer id, String nombre, Short idTipo, String tipo, String ubicacion,
                             Short aforo, Short duracionFranjaMin, String descripcion) {

    public static InstalacionDto from(Instalacion i) {
        return new InstalacionDto(i.getId(), i.getNombre(), i.getTipo().getId(), i.getTipo().getNombre(),
                i.getUbicacion(), i.getAforo(), i.getDuracionFranjaMin(), i.getDescripcion());
    }
}
