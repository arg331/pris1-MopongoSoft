package es.ual.sadual.controller;

import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaRequest;
import es.ual.sadual.service.ReservaService;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** F04 · Reservar y F05 · Mis reservas / cancelación. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaDto> crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReservaRequest peticion) {
        ReservaDto creada = reservaService.crear(UsuarioActual.id(jwt), peticion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservas/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/mias")
    public List<ReservaDto> mias(@AuthenticationPrincipal Jwt jwt) {
        return reservaService.delUsuario(UsuarioActual.id(jwt));
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaDto cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return reservaService.cancelar(UsuarioActual.id(jwt), UsuarioActual.esAdmin(jwt), id);
    }
}
