package es.ual.sadual.controller;

import es.ual.sadual.dto.InstalacionAdminDto;
import es.ual.sadual.dto.InstalacionRequest;
import es.ual.sadual.service.AdminInstalacionService;
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

/** F06 · Alta, edición y baja de instalaciones y sus horarios (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/instalaciones")
public class AdminInstalacionController {

    private final AdminInstalacionService servicio;

    public AdminInstalacionController(AdminInstalacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<InstalacionAdminDto> todas() {
        return servicio.todas();
    }

    @PostMapping
    public ResponseEntity<InstalacionAdminDto> crear(@Valid @RequestBody InstalacionRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(peticion));
    }

    @PutMapping("/{id}")
    public InstalacionAdminDto editar(@PathVariable Integer id, @Valid @RequestBody InstalacionRequest peticion) {
        return servicio.editar(id, peticion);
    }

    @PatchMapping("/{id}/desactivar")
    public InstalacionAdminDto desactivar(@PathVariable Integer id) {
        return servicio.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public InstalacionAdminDto activar(@PathVariable Integer id) {
        return servicio.activar(id);
    }
}
