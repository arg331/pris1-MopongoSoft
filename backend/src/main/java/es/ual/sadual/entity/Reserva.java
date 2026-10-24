package es.ual.sadual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_instalacion", nullable = false)
    private Instalacion instalacion;

    /** Reserva institucional (Variación 7, Incremento 2). En V1 siempre null. */
    @Column(name = "id_entidad")
    private Integer idEntidad;

    /** Administrador que autoriza (Variación 7, Incremento 2). En V1 siempre null. */
    @Column(name = "id_autorizador")
    private Integer idAutorizador;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    protected Reserva() {
    }

    public static Reserva confirmada(Usuario usuario, Instalacion instalacion, LocalDate fecha,
                                     LocalTime horaInicio, LocalTime horaFin, LocalDateTime ahora) {
        Reserva r = new Reserva();
        r.usuario = usuario;
        r.instalacion = instalacion;
        r.fecha = fecha;
        r.horaInicio = horaInicio;
        r.horaFin = horaFin;
        r.estado = EstadoReserva.CONFIRMADA;
        r.fechaCreacion = ahora;
        return r;
    }

    public void cancelar(LocalDateTime ahora) {
        this.estado = EstadoReserva.CANCELADA;
        this.fechaCancelacion = ahora; // exigido por ck_reserva_cancelacion
    }

    /** P3: el trigger tg_sancionar_no_presentado crea la sanción al guardar este estado. */
    public void marcarNoPresentado() {
        this.estado = EstadoReserva.NO_PRESENTADO;
    }

    public LocalDateTime inicio() {
        return LocalDateTime.of(fecha, horaInicio);
    }

    public boolean isActiva() {
        return estado != null && estado.ocupaFranja();
    }

    public boolean esDe(Integer idUsuario) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(idUsuario);
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Instalacion getInstalacion() { return instalacion; }
    public Integer getIdEntidad() { return idEntidad; }
    public Integer getIdAutorizador() { return idAutorizador; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
}
