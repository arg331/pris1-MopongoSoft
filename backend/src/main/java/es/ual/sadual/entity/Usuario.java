package es.ual.sadual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    // EAGER (por defecto en @ManyToOne): el rol se necesita en casi todas las operaciones.
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    /** P17: entidad con convenio a la que representa (solo perfil EXTERNO, tg_validar_representante). */
    @Column(name = "id_entidad")
    private Integer idEntidad;

    protected Usuario() {
    }

    public Usuario(Rol rol, String nombre, String apellidos, String email, String passwordHash) {
        this.rol = rol;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** P17: alta por autorregistro de un tutor (sin entidad) o de un representante (con entidad). */
    public static Usuario externoRegistrado(Rol rolExterno, String nombre, String apellidos, String email,
                                            String passwordHash, String telefono, Integer idEntidad,
                                            LocalDateTime ahora) {
        Usuario u = new Usuario(rolExterno, nombre, apellidos, email, passwordHash);
        u.telefono = telefono;
        u.idEntidad = idEntidad;
        u.fechaAlta = ahora;
        // P23: el representante queda pendiente hasta que la administración lo valide y lo active
        u.activo = idEntidad == null;
        return u;
    }

    /** F07 · P16: la comunidad universitaria (y el personal de administración) la da de alta la administración. */
    public static Usuario altaPorAdministracion(Rol rol, String nombre, String apellidos, String email,
                                                String passwordHash, String telefono, LocalDateTime ahora) {
        Usuario u = new Usuario(rol, nombre, apellidos, email, passwordHash);
        u.telefono = telefono;
        u.fechaAlta = ahora;
        return u;
    }

    /** F07 · Cambio de perfil. Un usuario con entidad solo puede ser EXTERNO (tg_validar_representante). */
    public void cambiarRol(Rol rol) {
        this.rol = rol;
    }

    public boolean esRepresentante() {
        return idEntidad != null;
    }

    @PrePersist
    void alPersistir() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
    }

    public Integer getId() { return id; }
    public Rol getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getTelefono() { return telefono; }
    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public Integer getIdEntidad() { return idEntidad; }

    public boolean isActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
