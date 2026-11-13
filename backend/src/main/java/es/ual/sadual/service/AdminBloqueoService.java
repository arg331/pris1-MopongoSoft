package es.ual.sadual.service;

import es.ual.sadual.dto.BloqueoDto;
import es.ual.sadual.dto.BloqueoRequest;
import es.ual.sadual.dto.CierreRequest;
import es.ual.sadual.dto.ResultadoBloqueoDto;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.AdminRepository;
import es.ual.sadual.repository.AdminRepository.BloqueoBd;
import es.ual.sadual.repository.FranjaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * F08 · Bloqueos y cierres.
 * <ul>
 *   <li>P7 · Solo la administración bloquea; el bloqueo tiene prioridad y cancela las reservas que
 *       coinciden (lo hace el trigger tg_bloqueo_cancela_reservas).</li>
 *   <li>P11 · Festivos y cierres: un cierre general bloquea todas las instalaciones activas
 *       (fn_cerrar_instalaciones, que respeta los bloqueos previos y cubre los huecos, INC-04).</li>
 *   <li>Los bloqueos de eventos y escuelas se gestionan desde sus módulos.</li>
 *   <li>Al borrar un bloqueo, la franja vuelve a estar libre; las reservas que canceló no se
 *       recuperan (❓P50).</li>
 * </ul>
 */
@Service
public class AdminBloqueoService {

    private final AdminRepository admin;
    private final InstalacionService instalacionService;
    private final FranjaRepository franjaRepository;
    private final Clock reloj;

    public AdminBloqueoService(AdminRepository admin, InstalacionService instalacionService,
                               FranjaRepository franjaRepository, Clock reloj) {
        this.admin = admin;
        this.instalacionService = instalacionService;
        this.franjaRepository = franjaRepository;
        this.reloj = reloj;
    }

    /** Por defecto, los bloqueos desde hoy y durante los próximos 60 días. */
    @Transactional(readOnly = true)
    public List<BloqueoDto> listar(Integer idInstalacion, LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now(reloj);
        LocalDate h = hasta != null ? hasta : d.plusDays(60);
        if (h.isBefore(d)) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "La fecha final no puede ser anterior a la inicial");
        }
        return admin.bloqueos(idInstalacion, d.atStartOfDay(), h.plusDays(1).atStartOfDay()).stream()
                .map(AdminBloqueoService::dto).toList();
    }

    @Transactional
    public ResultadoBloqueoDto crear(Integer idAdmin, BloqueoRequest p) {
        validarPeriodo(p.inicio(), p.fin());
        var instalacion = instalacionService.activa(p.idInstalacion());
        franjaRepository.bloquearInstalacion(instalacion.getId());
        List<String> ocupada = admin.motivosSolapados(instalacion.getId(), p.inicio(), p.fin());
        if (!ocupada.isEmpty()) {
            throw new ConflictoException("INSTALACION_NO_DISPONIBLE", instalacion.getNombre()
                    + " ya está bloqueada en ese periodo (" + String.join(", ", ocupada) + ")");
        }
        int afectadas = admin.reservasActivasSolapadas(instalacion.getId(), p.inicio(), p.fin());
        Integer id = admin.insertarBloqueo(instalacion.getId(), idAdmin, p.inicio(), p.fin(), p.motivo(),
                p.observaciones() == null || p.observaciones().isBlank() ? null : p.observaciones().trim());
        return new ResultadoBloqueoDto(1, afectadas, dto(admin.bloqueo(id).orElseThrow()));
    }

    @Transactional
    public void eliminar(Integer idBloqueo) {
        BloqueoBd b = admin.bloqueo(idBloqueo).orElseThrow(() ->
                new RecursoNoEncontradoException("BLOQUEO_NO_ENCONTRADO", "El bloqueo no existe"));
        if (b.idEvento() != null || b.idEscuela() != null) {
            throw new ConflictoException("BLOQUEO_GESTIONADO", "Este bloqueo pertenece a "
                    + (b.idEvento() != null ? "un evento" : "una escuela") + ": se gestiona desde su módulo");
        }
        admin.borrarBloqueo(idBloqueo);
    }

    /** P11 · Cierre general (festivos, vacaciones, cierres extraordinarios). */
    @Transactional
    public ResultadoBloqueoDto cerrar(Integer idAdmin, CierreRequest p) {
        validarPeriodo(p.inicio(), p.fin());
        int afectadas = admin.reservasActivasSolapadas(null, p.inicio(), p.fin());
        int creados = admin.cerrarInstalaciones(p.inicio(), p.fin(), p.observaciones().trim(), idAdmin);
        return new ResultadoBloqueoDto(creados, afectadas, null);
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "El fin debe ser posterior al inicio");
        }
        if (!fin.isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "El periodo ya ha terminado");
        }
    }

    private static BloqueoDto dto(BloqueoBd b) {
        String gestionado = b.idEvento() != null ? "EVENTO" : b.idEscuela() != null ? "ESCUELA" : null;
        return new BloqueoDto(b.id(), b.idInstalacion(), b.instalacion(), b.inicio(), b.fin(), b.motivo(),
                b.observaciones(), b.creador(), gestionado);
    }
}
