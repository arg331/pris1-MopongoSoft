package es.ual.sadual.controller;

import es.ual.sadual.dto.BloqueoDto;
import es.ual.sadual.dto.BloqueoRequest;
import es.ual.sadual.dto.CierreRequest;
import es.ual.sadual.dto.ResultadoBloqueoDto;
import es.ual.sadual.service.AdminBloqueoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** F08 · Bloqueos por instalación (P7) y cierres generales (P11). Solo ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminBloqueoController {

    private final AdminBloqueoService servicio;

    public AdminBloqueoController(AdminBloqueoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/bloqueos")
    public List<BloqueoDto> listar(
            @RequestParam(required = false) Integer idInstalacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.listar(idInstalacion, desde, hasta);
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<ResultadoBloqueoDto> crear(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody BloqueoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(UsuarioActual.id(jwt), peticion));
    }

    @DeleteMapping("/bloqueos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cierres")
    public ResponseEntity<ResultadoBloqueoDto> cerrar(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CierreRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.cerrar(UsuarioActual.id(jwt), peticion));
    }
}
