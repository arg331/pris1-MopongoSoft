package es.ual.sadual.entity;

import java.util.EnumSet;
import java.util.Set;

/** Valores admitidos por ck_reserva_estado. PENDIENTE y CONFIRMADA ocupan la franja. */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    NO_PRESENTADO,
    FINALIZADA;

    /** Estados que ocupan franja y cuentan para el límite diario (P1). */
    public static final Set<EstadoReserva> ACTIVOS = EnumSet.of(PENDIENTE, CONFIRMADA);

    public boolean ocupaFranja() {
        return ACTIVOS.contains(this);
    }
}
