package es.ual.sadual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Espacio reservable de forma independiente (p. ej. "Pista de pádel 1"). */
@Entity
@Table(name = "instalacion")
public class Instalacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instalacion")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoInstalacion tipo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "ubicacion", length = 120)
    private String ubicacion;

    @Column(name = "aforo", nullable = false)
    private Short aforo;

    @Column(name = "duracion_franja_min", nullable = false)
    private Short duracionFranjaMin;

    @Column(name = "activa", nullable = false)
    private Boolean activa = Boolean.TRUE;

    @Column(name = "descripcion")
    private String descripcion;

    protected Instalacion() {
    }

    public Instalacion(TipoInstalacion tipo, String nombre, short aforo, short duracionFranjaMin) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.aforo = aforo;
        this.duracionFranjaMin = duracionFranjaMin;
    }

    /** F06 · Edición desde el panel. P12: cambiar la duración no altera las reservas ya hechas. */
    public void actualizar(TipoInstalacion tipo, String nombre, String ubicacion, short aforo,
                           short duracionFranjaMin, String descripcion) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.aforo = aforo;
        this.duracionFranjaMin = duracionFranjaMin;
        this.descripcion = descripcion;
    }

    public Integer getId() { return id; }
    public TipoInstalacion getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public String getUbicacion() { return ubicacion; }
    public Short getAforo() { return aforo; }
    public Short getDuracionFranjaMin() { return duracionFranjaMin; }
    public String getDescripcion() { return descripcion; }

    public boolean isActiva() {
        return Boolean.TRUE.equals(activa);
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
