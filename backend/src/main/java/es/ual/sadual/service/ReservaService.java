package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.FranjaDto;
import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaRequest;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.Reserva;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.ReservaRepository;
import es.ual.sadual.repository.SancionRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * F04 · Reserva de una franja y F05 · Mis reservas y cancelación.
 * <p>
 * Reglas validadas por el cliente el 25/10/2026 (docs/requisitos-cliente.md):
 * <ul>
 *   <li>P4/P17 · solo la comunidad universitaria hace reservas individuales; los externos
 *       (tutores y representantes de entidades) no.</li>
 *   <li>P2 · al menos 3 horas de antelación, sin máximo.</li>
 *   <li>P3 · quien tiene una sanción vigente no puede reservar.</li>
 *   <li>P1 · una sola reserva activa por usuario y día.</li>
 *   <li>P10 · cero dobles reservas.</li>
 * </ul>
 * Las reglas que dependen de "ahora" se comprueban aquí, con el reloj inyectable. Las de
 * integridad (P1 y P10) las garantiza además la base de datos: índice uq_reserva_usuario_dia y
 * restricción ex_reserva_sin_solape (GlobalExceptionHandler las traduce a 409). Las peticiones
 * simultáneas sobre una misma instalación se ponen en fila con un candado de transacción.
 */
@Service
public class ReservaService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final SancionRepository sanciones;
    private final InstalacionService instalacionService;
    private final DisponibilidadService disponibilidadService;
    private final FranjaRepository franjaRepository;
    private final PoliticaReservas politica;
    private final Clock reloj;

    public ReservaService(ReservaRepository reservas, UsuarioRepository usuarios, SancionRepository sanciones,
                          InstalacionService instalacionService, DisponibilidadService disponibilidadService,
                          FranjaRepository franjaRepository, PoliticaReservas politica, Clock reloj) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.sanciones = sanciones;
        this.instalacionService = instalacionService;
        this.disponibilidadService = disponibilidadService;
        this.franjaRepository = franjaRepository;
        this.politica = politica;
        this.reloj = reloj;
    }

    @Transactional
    public ReservaDto crear(Integer idUsuario, ReservaRequest peticion) {
        Usuario usuario = usuarios.findById(idUsuario)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_NO_VALIDO", "Tu usuario no puede reservar"));
        if (!politica.puedeReservar(usuario.getRol().getCodigo())) {
            throw new AccesoDenegadoException("PERFIL_NO_AUTORIZADO",
                    "Las reservas individuales son solo para la comunidad universitaria");
        }
        Instalacion instalacion = instalacionService.activa(peticion.idInstalacion());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime inicio = LocalDateTime.of(peticion.fecha(), peticion.horaInicio());

        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("RESERVA_EN_PASADO", "No se puede reservar una franja que ya ha empezado");
        }
        if (inicio.isBefore(ahora.plus(politica.antelacionMinima()))) {
            throw antelacionInsuficiente();
        }
        sanciones.vigenteEn(idUsuario, ahora.toLocalDate()).ifPresent(fin -> {
            throw new AccesoDenegadoException("USUARIO_SANCIONADO",
                    "No te presentaste a una reserva: no puedes reservar hasta el " + DIA.format(fin.plusDays(1)));
        });

        // A partir de aquí, una sola petición a la vez por instalación: las comprobaciones
        // siguientes ven lo que hayan confirmado las anteriores (sin deadlocks; ver FranjaRepository).
        franjaRepository.bloquearInstalacion(instalacion.getId());

        FranjaDto franja = disponibilidadService.franjas(instalacion, peticion.fecha()).stream()
                .filter(f -> f.horaInicio().equals(peticion.horaInicio()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("FRANJA_NO_VALIDA",
                        "La instalación no abre a esa hora o la hora no coincide con el inicio de una franja"));

        switch (franja.estado()) {
            case RESERVADA -> throw new ConflictoException("FRANJA_OCUPADA", "La franja ya está reservada");
            case BLOQUEADA -> throw new ConflictoException("FRANJA_BLOQUEADA",
                    "La instalación está bloqueada en esa franja (" + franja.motivo() + ")");
            case PASADA -> throw new ReglaNegocioException("RESERVA_EN_PASADO",
                    "No se puede reservar una franja que ya ha empezado");
            case FUERA_DE_PLAZO -> throw antelacionInsuficiente();
            case LIBRE -> { /* continúa */ }
        }

        if (reservas.existsByUsuarioIdAndFechaAndEstadoInAndIdEntidadIsNull(
                idUsuario, peticion.fecha(), EstadoReserva.ACTIVOS)) {
            throw new ConflictoException("LIMITE_DIARIO",
                    "Ya tienes una reserva ese día: solo se permite una reserva por día");
        }

        Reserva reserva = Reserva.confirmada(usuario, instalacion, peticion.fecha(),
                franja.horaInicio(), franja.horaFin(), ahora);
        // flush inmediato: si otra petición ganó la carrera, el error de la BD salta aquí y no en el commit
        reservas.saveAndFlush(reserva);
        return ReservaDto.from(reserva, ahora);
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> delUsuario(Integer idUsuario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return reservas.buscarDelUsuario(idUsuario).stream()
                .map(r -> ReservaDto.from(r, ahora))
                .toList();
    }

    @Transactional
    public ReservaDto cancelar(Integer idUsuario, boolean esAdmin, Integer idReserva) {
        Reserva reserva = buscar(idReserva);
        if (!esAdmin && !reserva.esDe(idUsuario)) {
            throw new AccesoDenegadoException("RESERVA_AJENA", "Solo puedes cancelar tus propias reservas");
        }
        if (!reserva.isActiva()) {
            throw new ConflictoException("RESERVA_NO_CANCELABLE", "La reserva ya está cancelada o finalizada");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!reserva.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("RESERVA_INICIADA", "No se puede cancelar una reserva que ya ha empezado");
        }
        reserva.cancelar(ahora);
        reservas.flush();
        return ReservaDto.from(reserva, ahora);
    }

    /** P3 · Solo administración. La BD genera la sanción de esa semana (tg_sancionar_no_presentado). */
    @Transactional
    public ReservaDto marcarNoPresentado(Integer idReserva) {
        Reserva reserva = buscar(idReserva);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoException("ESTADO_NO_VALIDO",
                    "Solo se puede marcar como no presentada una reserva confirmada");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (reserva.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("RESERVA_NO_INICIADA", "La reserva todavía no ha empezado");
        }
        reserva.marcarNoPresentado();
        reservas.flush();
        return ReservaDto.from(reserva, ahora);
    }

    private Reserva buscar(Integer idReserva) {
        return reservas.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("RESERVA_NO_ENCONTRADA", "La reserva no existe"));
    }

    private ReglaNegocioException antelacionInsuficiente() {
        return new ReglaNegocioException("ANTELACION_MINIMA",
                "Hay que reservar con al menos " + politica.antelacionMinimaHoras() + " horas de antelación");
    }
}
