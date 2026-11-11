package es.ual.sadual.controller;

import es.ual.sadual.dto.AltaUsuarioRequest;
import es.ual.sadual.dto.CambioRolRequest;
import es.ual.sadual.dto.UsuarioAdminDto;
import es.ual.sadual.service.AdminUsuarioService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F07 · Gestión de usuarios (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService servicio;

    public AdminUsuarioController(AdminUsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioAdminDto> buscar(@RequestParam(required = false) String rol,
                                        @RequestParam(required = false) String texto) {
        return servicio.buscar(rol, texto);
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminDto> alta(@Valid @RequestBody AltaUsuarioRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.alta(peticion));
    }

    @PatchMapping("/{id}/rol")
    public UsuarioAdminDto cambiarRol(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                                      @Valid @RequestBody CambioRolRequest peticion) {
        return servicio.cambiarRol(UsuarioActual.id(jwt), id, peticion.rol());
    }

    @PatchMapping("/{id}/activar")
    public UsuarioAdminDto activar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return servicio.cambiarActivo(UsuarioActual.id(jwt), id, true);
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioAdminDto desactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return servicio.cambiarActivo(UsuarioActual.id(jwt), id, false);
    }
}
