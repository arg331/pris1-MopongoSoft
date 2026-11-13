package es.ual.sadual.controller;

import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaInstitucionalDto;
import es.ual.sadual.dto.ResolucionRequest;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.service.ReservaInstitucionalService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** P21 · Bandeja de autorización de reservas institucionales (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/reservas-institucionales")
public class AdminReservaInstitucionalController {

    private final ReservaInstitucionalService servicio;

    public AdminReservaInstitucionalController(ReservaInstitucionalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ReservaInstitucionalDto> listar(@RequestParam(required = false) EstadoReserva estado) {
        return servicio.listar(estado);
    }

    @PatchMapping("/{id}/autorizar")
    public ReservaDto autorizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return servicio.autorizar(UsuarioActual.id(jwt), id);
    }

    @PatchMapping("/{id}/rechazar")
    public ReservaDto rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                               @Valid @RequestBody ResolucionRequest peticion) {
        return servicio.rechazar(UsuarioActual.id(jwt), id, peticion.motivo());
    }
}
