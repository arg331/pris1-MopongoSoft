package es.ual.sadual.controller;

import es.ual.sadual.dto.EscuelaDto;
import es.ual.sadual.dto.EscuelaRequest;
import es.ual.sadual.dto.InscritoEscuelaDto;
import es.ual.sadual.dto.ResultadoEscuelaDto;
import es.ual.sadual.service.EscuelaService;
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

/** P19 · Gestión de escuelas por la administración (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/escuelas")
public class AdminEscuelaController {

    private final EscuelaService servicio;

    public AdminEscuelaController(EscuelaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EscuelaDto> todas() {
        return servicio.todas();
    }

    @PostMapping
    public ResponseEntity<ResultadoEscuelaDto> crear(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody EscuelaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(UsuarioActual.id(jwt), peticion));
    }

    /** ❓P56 · Cancela la escuela: libera sus sesiones futuras y da de baja a los inscritos. */
    @PatchMapping("/{id}/cancelar")
    public ResultadoEscuelaDto cancelar(@PathVariable Integer id) {
        return servicio.cancelar(id);
    }

    @GetMapping("/{id}/inscritos")
    public List<InscritoEscuelaDto> inscritos(@PathVariable Integer id) {
        return servicio.inscritos(id);
    }
}
