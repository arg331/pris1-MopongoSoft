package es.ual.sadual.controller;

import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaInstitucionalRequest;
import es.ual.sadual.service.ReservaInstitucionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * P21 · El representante de una entidad solicita una reserva institucional (queda PENDIENTE).
 * Para consultarlas o cancelarlas usa los mismos endpoints que el resto: GET /api/reservas/mias y
 * PATCH /api/reservas/{id}/cancelar.
 */
@RestController
@RequestMapping("/api/reservas-institucionales")
public class ReservaInstitucionalController {

    private final ReservaInstitucionalService servicio;

    public ReservaInstitucionalController(ReservaInstitucionalService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ReservaDto> solicitar(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody ReservaInstitucionalRequest peticion) {
        ReservaDto creada = servicio.solicitar(UsuarioActual.id(jwt), peticion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservas/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }
}
