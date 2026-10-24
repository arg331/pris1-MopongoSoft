package es.ual.sadual.controller;

import es.ual.sadual.dto.DisponibilidadDto;
import es.ual.sadual.dto.InstalacionDto;
import es.ual.sadual.dto.TipoInstalacionDto;
import es.ual.sadual.service.DisponibilidadService;
import es.ual.sadual.service.InstalacionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** F02 · Catálogo y F03 · Disponibilidad. */
@RestController
@RequestMapping("/api")
public class InstalacionController {

    private final InstalacionService instalacionService;
    private final DisponibilidadService disponibilidadService;

    public InstalacionController(InstalacionService instalacionService, DisponibilidadService disponibilidadService) {
        this.instalacionService = instalacionService;
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/tipos-instalacion")
    public List<TipoInstalacionDto> tipos() {
        return instalacionService.tipos();
    }

    @GetMapping("/instalaciones")
    public List<InstalacionDto> catalogo(@RequestParam(name = "tipo", required = false) Short idTipo) {
        return instalacionService.catalogo(idTipo);
    }

    @GetMapping("/instalaciones/{id}")
    public InstalacionDto detalle(@PathVariable Integer id) {
        return instalacionService.detalle(id);
    }

    @GetMapping("/instalaciones/{id}/disponibilidad")
    public DisponibilidadDto disponibilidad(@PathVariable Integer id,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidadService.consultar(id, fecha);
    }
}
