package es.ual.sadual.service;

import es.ual.sadual.dto.AltaUsuarioRequest;
import es.ual.sadual.dto.UsuarioAdminDto;
import es.ual.sadual.entity.Rol;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import org.springframework.jdbc.core.JdbcTemplate;
import es.ual.sadual.repository.RolRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F07 · Gestión de usuarios (núcleo común obligatorio).
 * <ul>
 *   <li>P16 · La administración da de alta a la comunidad universitaria (sin autorregistro).</li>
 *   <li>G4 · Lista usuarios, cambia su perfil y los activa o desactiva.</li>
 *   <li>Desactivar a un usuario no toca sus reservas (❓P49). Un administrador no puede desactivarse
 *       ni quitarse el perfil a sí mismo (evita quedarse sin acceso al panel).</li>
 * </ul>
 */
@Service
public class AdminUsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public AdminUsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwordEncoder,
                               JdbcTemplate jdbc, Clock reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDto> buscar(String rol, String texto) {
        return consultar(rol, texto, null);
    }

    private List<UsuarioAdminDto> consultar(String rol, String texto, Integer id) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.id_usuario, u.nombre, u.apellidos, u.email, u.telefono, r.codigo, r.nombre AS rol_nombre,
                       u.activo, u.fecha_alta, u.id_entidad, e.nombre AS entidad,
                       (SELECT count(*) FROM reserva x WHERE x.id_usuario = u.id_usuario
                           AND x.estado IN ('PENDIENTE','CONFIRMADA') AND x.fecha + x.hora_inicio > ?) AS futuras
                  FROM usuario u
                  JOIN rol r ON r.id_rol = u.id_rol
                  LEFT JOIN entidad_convenio e ON e.id_entidad = u.id_entidad
                 WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>(List.of(LocalDateTime.now(reloj)));
        if (rol != null && !rol.isBlank()) {
            sql.append(" AND r.codigo = ?");
            args.add(rol.trim().toUpperCase(Locale.ROOT));
        }
        if (id != null) {
            sql.append(" AND u.id_usuario = ?");
            args.add(id);
        }
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (u.nombre || ' ' || u.apellidos || ' ' || u.email) ILIKE ?");
            args.add("%" + texto.trim() + "%");
        }
        sql.append(" ORDER BY u.apellidos, u.nombre LIMIT 1000");
        return jdbc.query(sql.toString(), (rs, n) -> new UsuarioAdminDto(rs.getInt("id_usuario"), rs.getString("nombre"),
                rs.getString("apellidos"), rs.getString("email"), rs.getString("telefono"), rs.getString("codigo"),
                rs.getString("rol_nombre"), rs.getBoolean("activo"), rs.getObject("fecha_alta", LocalDateTime.class),
                (Integer) rs.getObject("id_entidad"), rs.getString("entidad"), rs.getInt("futuras")), args.toArray());
    }

    @Transactional
    public UsuarioAdminDto alta(AltaUsuarioRequest p) {
        String email = p.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictoException("EMAIL_EN_USO", "Ya existe una cuenta con ese email");
        }
        Usuario nuevo = Usuario.altaPorAdministracion(rol(p.rol()), p.nombre().trim(), p.apellidos().trim(), email,
                passwordEncoder.encode(p.password()), p.telefono() == null || p.telefono().isBlank() ? null
                        : p.telefono().trim(), LocalDateTime.now(reloj));
        usuarios.saveAndFlush(nuevo);
        return uno(nuevo.getId());
    }

    @Transactional
    public UsuarioAdminDto cambiarRol(Integer idAdmin, Integer idUsuario, String codigoRol) {
        Usuario u = buscarUsuario(idUsuario);
        if (u.getId().equals(idAdmin) && !"ADMIN".equals(codigoRol)) {
            throw new ReglaNegocioException("OPERACION_SOBRE_SI_MISMO", "No puedes quitarte el perfil de administración");
        }
        if (u.esRepresentante() && !"EXTERNO".equals(codigoRol)) {
            throw new ReglaNegocioException("REPRESENTANTE_NO_VALIDO",
                    "Este usuario representa a una entidad con convenio: solo puede tener perfil externo (P17)");
        }
        u.cambiarRol(rol(codigoRol));
        usuarios.flush();
        return uno(idUsuario);
    }

    @Transactional
    public UsuarioAdminDto cambiarActivo(Integer idAdmin, Integer idUsuario, boolean activo) {
        Usuario u = buscarUsuario(idUsuario);
        if (u.getId().equals(idAdmin) && !activo) {
            throw new ReglaNegocioException("OPERACION_SOBRE_SI_MISMO", "No puedes desactivar tu propia cuenta");
        }
        u.setActivo(activo);
        usuarios.flush();
        return uno(idUsuario);
    }

    private UsuarioAdminDto uno(Integer id) {
        return consultar(null, null, id).getFirst();
    }

    private Usuario buscarUsuario(Integer id) {
        return usuarios.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "El usuario no existe"));
    }

    private Rol rol(String codigo) {
        return roles.findByCodigo(codigo).orElseThrow(() ->
                new ReglaNegocioException("PERFIL_NO_VALIDO", "El perfil no existe"));
    }
}
