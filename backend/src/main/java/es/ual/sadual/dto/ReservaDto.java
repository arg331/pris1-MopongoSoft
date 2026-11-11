package es.ual.sadual.dto;

import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.entity.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * institucional y motivoRechazo (P21, Incremento 2) permiten al representante ver en "Mis reservas"
 * si su reserva está pendiente de autorizar, confirmada o rechazada y por qué.
 */
public record ReservaDto(Integer id, Integer idInstalacion, String instalacion, String tipo,
                         LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                         EstadoReserva estado, boolean cancelable, boolean institucional,
                         String motivoRechazo) {

    /** Debe llamarse dentro de una transacción (instalación y tipo son LAZY). */
    public static ReservaDto from(Reserva r, LocalDateTime ahora) {
        boolean cancelable = r.isActiva() && r.inicio().isAfter(ahora);
        return new ReservaDto(r.getId(), r.getInstalacion().getId(), r.getInstalacion().getNombre(),
                r.getInstalacion().getTipo().getNombre(), r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
                r.getEstado(), cancelable, r.isInstitucional(), r.getMotivoRechazo());
    }
}
