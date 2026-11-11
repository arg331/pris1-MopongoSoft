package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.LoginRequest;
import es.ual.sadual.dto.LoginResponse;
import es.ual.sadual.dto.RegistroRequest;
import es.ual.sadual.dto.RegistroResponse;
import es.ual.sadual.dto.UsuarioDto;
import es.ual.sadual.entity.Rol;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.CredencialesInvalidasException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.EntidadRepository;
import es.ual.sadual.repository.RolRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/** F01 · Inicio de sesión con perfiles y P17 · autorregistro de tutores y representantes. */
@Service
public class AuthService {

    /** Hash bcrypt de relleno: iguala el tiempo de respuesta cuando el email no existe. */
    private static final String HASH_FICTICIO = "$2a$10$60NJi.4Fg2V/Hrdp.WEMruwGTqS7tnM46xPNYWI0i1y8AiGtvDiRC";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PoliticaReservas politica;
    private final RolRepository roles;
    private final EntidadRepository entidades;
    private final Clock reloj;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService,
                       PoliticaReservas politica, RolRepository roles, EntidadRepository entidades, Clock reloj) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.politica = politica;
        this.roles = roles;
        this.entidades = entidades;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest peticion) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(peticion.email().trim()).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(peticion.password(), HASH_FICTICIO);
            throw new CredencialesInvalidasException();
        }
        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo()) {
            // Credenciales correctas pero cuenta inactiva: se explica (P23, representante pendiente de validar)
            throw new AccesoDenegadoException("CUENTA_INACTIVA", usuario.esRepresentante()
                    ? "Tu cuenta de representante está pendiente de que la administración la valide"
                    : "Tu cuenta está desactivada. Contacta con el Servicio de Deportes");
        }
        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        return new LoginResponse(token.token(), token.expiraEn(), dto(usuario));
    }

    /**
     * P17 · Un tutor o un representante se registra como usuario EXTERNO.
     * <ul>
     *   <li>P24 · El teléfono es obligatorio para los tutores y opcional para los representantes.</li>
     *   <li>P23 · El tutor queda con la sesión iniciada. El representante indica el CIF de su entidad (con
     *       convenio vigente) y su cuenta queda pendiente hasta que la administración la active.</li>
     * </ul>
     * La unicidad del email la garantiza además la BD (usuario_email_key → 409 EMAIL_EN_USO).
     */
    @Transactional
    public RegistroResponse registrar(RegistroRequest peticion) {
        String email = peticion.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictoException("EMAIL_EN_USO", "Ya existe una cuenta con ese email");
        }
        if (peticion.tipo() == RegistroRequest.TipoRegistro.TUTOR
                && (peticion.telefono() == null || peticion.telefono().isBlank())) {
            throw new ReglaNegocioException("TELEFONO_OBLIGATORIO", "Los tutores deben indicar un teléfono de contacto");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Integer idEntidad = null;
        if (peticion.tipo() == RegistroRequest.TipoRegistro.REPRESENTANTE) {
            if (peticion.cifEntidad() == null || peticion.cifEntidad().isBlank()) {
                throw new ReglaNegocioException("ENTIDAD_OBLIGATORIA",
                        "Para registrarte como representante indica el CIF de tu entidad");
            }
            idEntidad = entidades.porCif(peticion.cifEntidad().trim())
                    .filter(e -> e.vigenteEn(ahora.toLocalDate()))
                    .orElseThrow(() -> new ReglaNegocioException("ENTIDAD_NO_VALIDA",
                            "No hay ninguna entidad con convenio vigente con ese CIF"))
                    .id();
        }
        Rol externo = roles.findByCodigo("EXTERNO")
                .orElseThrow(() -> new IllegalStateException("Falta el perfil EXTERNO en la tabla rol"));
        Usuario nuevo = Usuario.externoRegistrado(externo, peticion.nombre().trim(), peticion.apellidos().trim(),
                email, passwordEncoder.encode(peticion.password()), blancoANulo(peticion.telefono()),
                idEntidad, ahora);
        usuarios.saveAndFlush(nuevo);
        if (!nuevo.isActivo()) {
            return new RegistroResponse(null, null, dto(nuevo), true);
        }
        TokenService.TokenEmitido token = tokenService.emitir(nuevo);
        return new RegistroResponse(token.token(), token.expiraEn(), dto(nuevo), false);
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Integer idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(Usuario::isActivo)
                .map(this::dto)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
    }

    private static String blancoANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private UsuarioDto dto(Usuario u) {
        return UsuarioDto.from(u, politica.puedeReservar(u.getRol().getCodigo()));
    }
}
