package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.LoginRequest;
import es.ual.sadual.dto.LoginResponse;
import es.ual.sadual.dto.UsuarioDto;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.CredencialesInvalidasException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F01 · Inicio de sesión con perfiles. */
@Service
public class AuthService {

    /** Hash bcrypt de relleno: iguala el tiempo de respuesta cuando el email no existe. */
    private static final String HASH_FICTICIO = "$2a$10$60NJi.4Fg2V/Hrdp.WEMruwGTqS7tnM46xPNYWI0i1y8AiGtvDiRC";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PoliticaReservas politica;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService,
                       PoliticaReservas politica) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.politica = politica;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest peticion) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(peticion.email().trim()).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(peticion.password(), HASH_FICTICIO);
            throw new CredencialesInvalidasException();
        }
        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash()) || !usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        return new LoginResponse(token.token(), token.expiraEn(), dto(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Integer idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(Usuario::isActivo)
                .map(this::dto)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
    }

    private UsuarioDto dto(Usuario u) {
        return UsuarioDto.from(u, politica.puedeReservar(u.getRol().getCodigo()));
    }
}
