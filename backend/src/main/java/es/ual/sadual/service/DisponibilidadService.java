package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.DisponibilidadDto;
import es.ual.sadual.dto.EstadoFranja;
import es.ual.sadual.dto.FranjaDto;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.FranjaRepository.BloqueoBd;
import es.ual.sadual.repository.FranjaRepository.FranjaBd;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * F03 · Consulta de disponibilidad.
 * Las franjas y su ocupación salen de fn_franjas_disponibles (misma lógica que la BD usa
 * para validar); aquí solo se añade el "por qué" de cada franja no libre.
 */
@Service
public class DisponibilidadService {

    private final InstalacionService instalacionService;
    private final FranjaRepository franjaRepository;
    private final PoliticaReservas politica;
    private final Clock reloj;

    public DisponibilidadService(InstalacionService instalacionService, FranjaRepository franjaRepository,
                                 PoliticaReservas politica, Clock reloj) {
        this.instalacionService = instalacionService;
        this.franjaRepository = franjaRepository;
        this.politica = politica;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public DisponibilidadDto consultar(Integer idInstalacion, LocalDate fecha) {
        Instalacion instalacion = instalacionService.activa(idInstalacion);
        return new DisponibilidadDto(instalacion.getId(), instalacion.getNombre(), fecha,
                instalacion.getDuracionFranjaMin(), politica.antelacionMinimaHoras(), franjas(instalacion, fecha));
    }

    /** Lista vacía si la instalación no abre ese día. */
    public List<FranjaDto> franjas(Instalacion instalacion, LocalDate fecha) {
        List<FranjaBd> franjas = franjaRepository.franjas(instalacion.getId(), fecha);
        if (franjas.isEmpty()) {
            return List.of();
        }
        List<BloqueoBd> bloqueos = franjaRepository.bloqueosDelDia(instalacion.getId(), fecha);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Duration antelacion = politica.antelacionMinima();
        return franjas.stream().map(f -> clasificar(f, fecha, bloqueos, ahora, antelacion)).toList();
    }

    static FranjaDto clasificar(FranjaBd franja, LocalDate fecha, List<BloqueoBd> bloqueos, LocalDateTime ahora,
                                Duration antelacionMinima) {
        LocalDateTime inicio = LocalDateTime.of(fecha, franja.horaInicio());
        LocalDateTime fin = LocalDateTime.of(fecha, franja.horaFin());

        Optional<BloqueoBd> bloqueo = bloqueos.stream()
                .filter(b -> b.inicio().isBefore(fin) && b.fin().isAfter(inicio))
                .findFirst();
        if (bloqueo.isPresent()) {
            return new FranjaDto(franja.horaInicio(), franja.horaFin(), EstadoFranja.BLOQUEADA, bloqueo.get().motivo());
        }
        if (!franja.disponible()) {
            return new FranjaDto(franja.horaInicio(), franja.horaFin(), EstadoFranja.RESERVADA, null);
        }
        if (!inicio.isAfter(ahora)) {
            return new FranjaDto(franja.horaInicio(), franja.horaFin(), EstadoFranja.PASADA, null);
        }
        if (inicio.isBefore(ahora.plus(antelacionMinima))) {
            return new FranjaDto(franja.horaInicio(), franja.horaFin(), EstadoFranja.FUERA_DE_PLAZO, null);
        }
        return new FranjaDto(franja.horaInicio(), franja.horaFin(), EstadoFranja.LIBRE, null);
    }
}
