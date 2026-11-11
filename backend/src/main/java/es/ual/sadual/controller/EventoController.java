package es.ual.sadual.controller;

import es.ual.sadual.dto.EventoDto;
import es.ual.sadual.service.EventoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** P18 · Eventos publicados e inscripción de los usuarios. */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService servicio;

    public EventoController(EventoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EventoDto> publicados(@AuthenticationPrincipal Jwt jwt) {
        return servicio.publicados(UsuarioActual.id(jwt));
    }

    @GetMapping("/mios")
    public List<EventoDto> mios(@AuthenticationPrincipal Jwt jwt) {
        return servicio.mios(UsuarioActual.id(jwt));
    }

    @PostMapping("/{id}/inscripcion")
    public EventoDto inscribir(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return servicio.inscribir(UsuarioActual.id(jwt), id);
    }

    @DeleteMapping("/{id}/inscripcion")
    public EventoDto anular(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return servicio.anularInscripcion(UsuarioActual.id(jwt), id);
    }
}
