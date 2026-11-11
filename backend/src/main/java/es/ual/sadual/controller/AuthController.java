package es.ual.sadual.controller;

import es.ual.sadual.dto.LoginRequest;
import es.ual.sadual.dto.LoginResponse;
import es.ual.sadual.dto.RegistroRequest;
import es.ual.sadual.dto.RegistroResponse;
import es.ual.sadual.dto.UsuarioDto;
import es.ual.sadual.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** F01 · Inicio y cierre de sesión. P17 · Autorregistro de usuarios externos. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest peticion) {
        return authService.login(peticion);
    }

    /**
     * P17 · Registro de tutores y representantes (perfil EXTERNO). El tutor recibe la sesión iniciada; el
     * representante queda pendiente de validación por la administración (P23).
     */
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponse> registro(@Valid @RequestBody RegistroRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(peticion));
    }

    /** JWT sin estado: el cliente descarta el token. El endpoint existe para auditar y para el contrato. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.perfil(UsuarioActual.id(jwt));
    }
}
