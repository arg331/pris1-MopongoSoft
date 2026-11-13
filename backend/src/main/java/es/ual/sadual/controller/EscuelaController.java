package es.ual.sadual.controller;

import es.ual.sadual.dto.ContactoMenorRequest;
import es.ual.sadual.dto.EscuelaDto;
import es.ual.sadual.dto.InscripcionEscuelaRequest;
import es.ual.sadual.dto.MenorDto;
import es.ual.sadual.dto.MenorRequest;
import es.ual.sadual.service.EscuelaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** P17, P19, P20, P22 · Catálogo de escuelas y gestión de menores e inscripciones por el tutor. */
@RestController
@RequestMapping("/api")
public class EscuelaController {

    private final EscuelaService servicio;

    public EscuelaController(EscuelaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/escuelas")
    public List<EscuelaDto> vigentes() {
        return servicio.vigentes();
    }

    @GetMapping("/menores")
    public List<MenorDto> menores(@AuthenticationPrincipal Jwt jwt) {
        return servicio.menores(UsuarioActual.id(jwt));
    }

    @PostMapping("/menores")
    public ResponseEntity<MenorDto> registrarMenor(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody MenorRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarMenor(UsuarioActual.id(jwt), peticion));
    }

    /** ❓P52 · Corrección del teléfono del tutor y del contacto de emergencia. */
    @PatchMapping("/menores/{id}/contacto")
    public MenorDto actualizarContacto(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                                       @Valid @RequestBody ContactoMenorRequest peticion) {
        return servicio.actualizarContacto(UsuarioActual.id(jwt), id, peticion);
    }

    /** P45 · Retirada del consentimiento RGPD: se eliminan los datos del menor y sus inscripciones. */
    @DeleteMapping("/menores/{id}")
    public ResponseEntity<Void> retirarConsentimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        servicio.retirarConsentimiento(UsuarioActual.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/escuelas/{id}/inscripciones")
    public ResponseEntity<MenorDto> inscribir(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                                              @Valid @RequestBody InscripcionEscuelaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.inscribir(UsuarioActual.id(jwt), id, peticion.idMenor()));
    }

    @PatchMapping("/escuelas/{id}/inscripciones/{idMenor}/baja")
    public MenorDto baja(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id, @PathVariable Integer idMenor) {
        return servicio.darDeBaja(UsuarioActual.id(jwt), id, idMenor);
    }
}
