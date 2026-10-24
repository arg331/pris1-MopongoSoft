package es.ual.sadual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Perfil de acceso (tabla rol). Los límites quedan parametrizados en BD hasta que el cliente responda a P1/P2. */
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Short id;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "max_reservas_semana", nullable = false)
    private Short maxReservasSemana;

    @Column(name = "antelacion_max_dias", nullable = false)
    private Short antelacionMaxDias;

    protected Rol() {
    }

    public Rol(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.maxReservasSemana = 3;
        this.antelacionMaxDias = 7;
    }

    public Short getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Short getMaxReservasSemana() { return maxReservasSemana; }
    public Short getAntelacionMaxDias() { return antelacionMaxDias; }

    public boolean esAdmin() {
        return "ADMIN".equals(codigo);
    }
}
