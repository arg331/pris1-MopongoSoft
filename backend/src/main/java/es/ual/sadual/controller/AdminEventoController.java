package es.ual.sadual.controller;

import es.ual.sadual.dto.EventoDto;
import es.ual.sadual.dto.EventoRequest;
import es.ual.sadual.dto.InscritoEventoDto;
import es.ual.sadual.dto.ResultadoEventoDto;
import es.ual.sadual.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** P18 · Gestión de eventos masivos (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/eventos")
public class AdminEventoController {

    private final EventoService servicio;

    public AdminEventoController(EventoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EventoDto> todos() {
        return servicio.todos();
    }

    @PostMapping
    public ResponseEntity<ResultadoEventoDto> crear(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody EventoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(UsuarioActual.id(jwt), peticion));
    }

    @PatchMapping("/{id}/publicar")
    public EventoDto publicar(@PathVariable Integer id) {
        return servicio.publicar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public ResultadoEventoDto cancelar(@PathVariable Integer id) {
        return servicio.cancelar(id);
    }

    @GetMapping("/{id}/inscritos")
    public List<InscritoEventoDto> inscritos(@PathVariable Integer id) {
        return servicio.inscritos(id);
    }
}
