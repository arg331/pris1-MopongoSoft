package es.ual.sadual.service;

import es.ual.sadual.dto.EventoDto;
import es.ual.sadual.dto.EventoRequest;
import es.ual.sadual.dto.InscritoEventoDto;
import es.ual.sadual.dto.ResultadoEventoDto;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.EventoRepository;
import es.ual.sadual.repository.EventoRepository.EventoBd;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * P18 · Eventos masivos (Variación 7).
 * <ul>
 *   <li>Solo los organiza la administración y pueden ocupar varias instalaciones: se crea un bloqueo
 *       EVENTO por instalación, que tiene prioridad y cancela las reservas que coincidan (P7).</li>
 *   <li>Ciclo de vida: PLANIFICADO → PUBLICADO (abre inscripciones) → FINALIZADO, o CANCELADO.</li>
 *   <li>Al cancelarlo se anulan las inscripciones y se liberan las instalaciones.</li>
 *   <li>El aforo máximo, único para el evento, lo garantiza la BD (tg_aforo_evento → 409 AFORO_COMPLETO).</li>
 *   <li>P33 · La administración decide en cada evento si se inscribe solo la comunidad universitaria o también
 *       los usuarios externos. La administración no se inscribe.</li>
 *   <li>P35 · Una inscripción por persona; inscripción y anulación hasta 24 horas antes del inicio.</li>
 * </ul>
 */
@Service
public class EventoService {

    private final EventoRepository eventos;
    private final UsuarioRepository usuarios;
    private final InstalacionService instalacionService;
    private final FranjaRepository franjaRepository;
    private final Duration cierreInscripcion;
    private final Clock reloj;

    public EventoService(EventoRepository eventos, UsuarioRepository usuarios, InstalacionService instalacionService,
                         FranjaRepository franjaRepository,
                         @Value("${sadual.eventos.cierre-inscripcion:PT24H}") Duration cierreInscripcion,
                         Clock reloj) {
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.instalacionService = instalacionService;
        this.franjaRepository = franjaRepository;
        this.cierreInscripcion = cierreInscripcion;
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- administración

    @Transactional
    public ResultadoEventoDto crear(Integer idAdmin, EventoRequest peticion) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!peticion.fin().isAfter(peticion.inicio())) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "El fin del evento debe ser posterior al inicio");
        }
        if (!peticion.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("EVENTO_EN_PASADO", "El evento debe empezar en el futuro");
        }
        // Orden fijo de candados: dos operaciones sobre las mismas instalaciones no se bloquean entre sí
        List<Instalacion> instalaciones = new ArrayList<>();
        for (Integer id : new TreeSet<>(peticion.idInstalaciones())) {
            Instalacion instalacion = instalacionService.activa(id);
            franjaRepository.bloquearInstalacion(id);
            List<String> ocupada = eventos.bloqueosSolapados(id, peticion.inicio(), peticion.fin());
            if (!ocupada.isEmpty()) {
                throw new ConflictoException("INSTALACION_NO_DISPONIBLE", instalacion.getNombre()
                        + " ya está bloqueada en ese periodo (" + String.join(", ", ocupada) + ")");
            }
            instalaciones.add(instalacion);
        }

        int reservasCanceladas = 0;
        Integer idEvento = eventos.insertar(idAdmin, peticion.nombre().trim(), peticion.descripcion(),
                peticion.inicio(), peticion.fin(), peticion.aforoMaximo(), peticion.publico());
        for (Instalacion instalacion : instalaciones) {
            reservasCanceladas += eventos.reservasActivasSolapadas(instalacion.getId(), peticion.inicio(), peticion.fin());
            eventos.bloquearInstalacion(idEvento, instalacion.getId(), idAdmin, peticion.inicio(), peticion.fin(),
                    peticion.nombre().trim());
        }
        return new ResultadoEventoDto(dto(buscar(idEvento), false), reservasCanceladas, 0);
    }

    @Transactional
    public EventoDto publicar(Integer idEvento) {
        EventoBd evento = eventos.buscarParaActualizar(idEvento).orElseThrow(EventoService::noEncontrado);
        if (!"PLANIFICADO".equals(evento.estado())) {
            throw new ConflictoException("ESTADO_NO_VALIDO", "Solo se publica un evento planificado");
        }
        if (!evento.inicio().isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("EVENTO_EN_PASADO", "El evento ya ha empezado");
        }
        eventos.cambiarEstado(idEvento, "PUBLICADO");
        return dto(buscar(idEvento), false);
    }

    /** P18: anula las inscripciones y libera las instalaciones. */
    @Transactional
    public ResultadoEventoDto cancelar(Integer idEvento) {
        EventoBd evento = eventos.buscarParaActualizar(idEvento).orElseThrow(EventoService::noEncontrado);
        if ("CANCELADO".equals(evento.estado()) || "FINALIZADO".equals(evento.estado())) {
            throw new ConflictoException("ESTADO_NO_VALIDO", "El evento ya está " + evento.estado().toLowerCase());
        }
        List<String> instalaciones = eventos.instalaciones(idEvento);
        eventos.cambiarEstado(idEvento, "CANCELADO");
        int anuladas = eventos.anularInscripciones(idEvento);
        eventos.borrarBloqueos(idEvento);
        EventoBd cancelado = buscar(idEvento);
        EventoDto dto = new EventoDto(cancelado.id(), cancelado.nombre(), cancelado.descripcion(), cancelado.inicio(),
                cancelado.fin(), cancelado.aforoMaximo(), cancelado.estado(), 0, 0, instalaciones, false,
                cancelado.publico(), cancelado.inicio().minus(cierreInscripcion));
        return new ResultadoEventoDto(dto, 0, anuladas);
    }

    @Transactional(readOnly = true)
    public List<EventoDto> todos() {
        return eventos.todos().stream().map(e -> dto(e, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<InscritoEventoDto> inscritos(Integer idEvento) {
        buscar(idEvento);
        return eventos.inscritos(idEvento);
    }

    // ---------------------------------------------------------------- usuarios

    /** P33: a los usuarios externos solo se les muestran los eventos abiertos a externos. */
    @Transactional(readOnly = true)
    public List<EventoDto> publicados(Integer idUsuario) {
        boolean externo = usuarios.findById(idUsuario).map(u -> "EXTERNO".equals(u.getRol().getCodigo())).orElse(false);
        return eventos.publicadosDesde(LocalDateTime.now(reloj)).stream()
                .filter(e -> !externo || "COMUNIDAD_Y_EXTERNOS".equals(e.publico()))
                .map(e -> dto(e, eventos.inscritoActivo(e.id(), idUsuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventoDto> mios(Integer idUsuario) {
        return eventos.delUsuario(idUsuario).stream().map(e -> dto(e, true)).toList();
    }

    @Transactional
    public EventoDto inscribir(Integer idUsuario, Integer idEvento) {
        Usuario usuario = usuarios.findById(idUsuario).filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_NO_VALIDO", "Tu usuario no está activo"));
        EventoBd evento = abiertoAInscripcion(idEvento);
        String rol = usuario.getRol().getCodigo();
        if ("ADMIN".equals(rol)) {
            throw new AccesoDenegadoException("PERFIL_NO_AUTORIZADO", "La administración organiza los eventos, no se inscribe");
        }
        if ("EXTERNO".equals(rol) && !"COMUNIDAD_Y_EXTERNOS".equals(evento.publico())) {
            throw new AccesoDenegadoException("PERFIL_NO_AUTORIZADO",
                    "Este evento es solo para la comunidad universitaria");
        }
        if (eventos.inscribir(idEvento, idUsuario, LocalDateTime.now(reloj)) == 0) {
            throw new ConflictoException("YA_INSCRITO", "Ya estás inscrito en este evento");
        }
        return dto(buscar(evento.id()), true);
    }

    @Transactional
    public EventoDto anularInscripcion(Integer idUsuario, Integer idEvento) {
        abiertoAInscripcion(idEvento);
        if (eventos.anularInscripcion(idEvento, idUsuario) == 0) {
            throw new ConflictoException("NO_INSCRITO", "No tienes inscripción activa en este evento");
        }
        return dto(buscar(idEvento), false);
    }

    // ---------------------------------------------------------------- apoyo

    private EventoBd abiertoAInscripcion(Integer idEvento) {
        EventoBd evento = buscar(idEvento);
        if (!"PUBLICADO".equals(evento.estado())) {
            throw new ConflictoException("EVENTO_NO_DISPONIBLE", "El evento no admite inscripciones");
        }
        if (!evento.inicio().minus(cierreInscripcion).isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("INSCRIPCION_CERRADA", "Las inscripciones se cierran "
                    + cierreInscripcion.toHours() + " horas antes del inicio del evento");
        }
        return evento;
    }

    private EventoBd buscar(Integer idEvento) {
        return eventos.buscar(idEvento).orElseThrow(EventoService::noEncontrado);
    }

    private EventoDto dto(EventoBd e, boolean inscrito) {
        return new EventoDto(e.id(), e.nombre(), e.descripcion(), e.inicio(), e.fin(), e.aforoMaximo(), e.estado(),
                e.inscritos(), Math.max(0, e.aforoMaximo() - e.inscritos()), eventos.instalaciones(e.id()), inscrito,
                e.publico(), e.inicio().minus(cierreInscripcion));
    }

    private static RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException("EVENTO_NO_ENCONTRADO", "El evento no existe");
    }
}
