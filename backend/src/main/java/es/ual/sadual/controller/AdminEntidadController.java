package es.ual.sadual.controller;

import es.ual.sadual.dto.EntidadAdminDto;
import es.ual.sadual.dto.EntidadRequest;
import es.ual.sadual.dto.SuspensionRequest;
import es.ual.sadual.service.AdminEntidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** P25 · Entidades con convenio y P32 · suspensión temporal (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/entidades")
public class AdminEntidadController {

    private final AdminEntidadService servicio;

    public AdminEntidadController(AdminEntidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EntidadAdminDto> todas() {
        return servicio.todas();
    }

    @PostMapping
    public ResponseEntity<EntidadAdminDto> crear(@Valid @RequestBody EntidadRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(peticion));
    }

    @PutMapping("/{id}")
    public EntidadAdminDto editar(@PathVariable Integer id, @Valid @RequestBody EntidadRequest peticion) {
        return servicio.editar(id, peticion);
    }

    @PatchMapping("/{id}/suspender")
    public EntidadAdminDto suspender(@PathVariable Integer id, @Valid @RequestBody SuspensionRequest peticion) {
        return servicio.suspender(id, peticion);
    }

    @PatchMapping("/{id}/levantar-suspension")
    public EntidadAdminDto levantarSuspension(@PathVariable Integer id) {
        return servicio.levantarSuspension(id);
    }
}
