package es.ual.sadual.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Reglas de reserva configurables (ver docs/requisitos-cliente.md).
 * <ul>
 *   <li>P4: perfiles que pueden hacer reservas individuales. EXTERNO queda fuera de momento,
 *       pero basta con añadirlo a sadual.reservas.perfiles-permitidos para habilitarlo.</li>
 *   <li>P2: antelación mínima para reservar (sin máximo).</li>
 * </ul>
 * La regla P1 (una reserva por día) la garantiza además el índice uq_reserva_usuario_dia.
 */
@Component
public class PoliticaReservas {

    private final Set<String> perfilesPermitidos;
    private final Duration antelacionMinima;

    public PoliticaReservas(
            @Value("${sadual.reservas.perfiles-permitidos:ALUMNADO,PDI,PAS,ADMIN}") Set<String> perfilesPermitidos,
            @Value("${sadual.reservas.antelacion-minima:PT3H}") Duration antelacionMinima) {
        this.perfilesPermitidos = Set.copyOf(perfilesPermitidos);
        this.antelacionMinima = antelacionMinima;
    }

    public boolean puedeReservar(String codigoRol) {
        return perfilesPermitidos.contains(codigoRol);
    }

    public Duration antelacionMinima() {
        return antelacionMinima;
    }

    public long antelacionMinimaHoras() {
        return antelacionMinima.toHours();
    }
}
