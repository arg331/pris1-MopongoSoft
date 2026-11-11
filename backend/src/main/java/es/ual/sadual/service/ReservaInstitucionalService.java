package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.FranjaDto;
import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaInstitucionalDto;
import es.ual.sadual.dto.ReservaInstitucionalRequest;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.Reserva;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.EntidadRepository;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.ReservaInstitucionalRepository;
import es.ual.sadual.repository.ReservaRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * P21 · Reservas institucionales (Variación 7).
 * <ul>
 *   <li>Las hace un representante (usuario EXTERNO vinculado a una entidad, P17) en nombre de su
 *       entidad, que debe tener el convenio vigente el día de la reserva.</li>
 *   <li>Nacen PENDIENTE y ya ocupan la franja; la administración las autoriza (CONFIRMADA) o las
 *       rechaza (RECHAZADA, la franja queda libre). Se guarda quién y cuándo resolvió.</li>
 *   <li>No cuentan para el límite de una reserva al día (índice uq_reserva_usuario_dia).</li>
 *   <li>P28 · Pueden abarcar varias franjas consecutivas, hasta 4 horas por reserva, sin límite semanal.</li>
 *   <li>P27 · Se solicitan con al menos 48 horas de antelación, dentro del horario de apertura.</li>
 *   <li>P26 · Sin plazo de autorización: quedan pendientes hasta que la administración las resuelve.</li>
 *   <li>P32 · Si la administración suspende a la entidad, no puede hacer nuevas reservas hasta la fecha fijada.</li>
 * </ul>
 */
@Service
public class ReservaInstitucionalService {

    private final ReservaRepository reservas;
    private final ReservaInstitucionalRepository consultas;
    private final UsuarioRepository usuarios;
    private final EntidadRepository entidades;
    private final InstalacionService instalacionService;
    private final DisponibilidadService disponibilidadService;
    private final FranjaRepository franjaRepository;
    private final PoliticaReservas politica;
    private final Duration antelacionMinima;
    private final Duration duracionMaxima;
    private final Clock reloj;

    public ReservaInstitucionalService(ReservaRepository reservas, ReservaInstitucionalRepository consultas,
                                       UsuarioRepository usuarios, EntidadRepository entidades,
                                       InstalacionService instalacionService,
                                       DisponibilidadService disponibilidadService,
                                       FranjaRepository franjaRepository, PoliticaReservas politica,
                                       @Value("${sadual.institucionales.antelacion-minima:PT48H}") Duration antelacionMinima,
                                       @Value("${sadual.institucionales.duracion-maxima:PT4H}") Duration duracionMaxima,
                                       Clock reloj) {
        this.reservas = reservas;
        this.consultas = consultas;
        this.usuarios = usuarios;
        this.entidades = entidades;
        this.instalacionService = instalacionService;
        this.disponibilidadService = disponibilidadService;
        this.franjaRepository = franjaRepository;
        this.politica = politica;
        this.antelacionMinima = antelacionMinima;
        this.duracionMaxima = duracionMaxima;
        this.reloj = reloj;
    }

    @Transactional
    public ReservaDto solicitar(Integer idUsuario, ReservaInstitucionalRequest peticion) {
        Usuario representante = usuarios.findById(idUsuario)
                .filter(Usuario::isActivo)
                .filter(Usuario::esRepresentante)
                .orElseThrow(() -> new AccesoDenegadoException("NO_REPRESENTANTE",
                        "Solo el representante de una entidad con convenio puede hacer reservas institucionales"));
        if (!peticion.horaFin().isAfter(peticion.horaInicio())) {
            throw new ReglaNegocioException("FRANJA_NO_VALIDA", "La hora de fin debe ser posterior a la de inicio");
        }
        if (Duration.between(peticion.horaInicio(), peticion.horaFin()).compareTo(duracionMaxima) > 0) {
            throw new ReglaNegocioException("DURACION_MAXIMA",
                    "Una reserva institucional puede durar como máximo " + duracionMaxima.toHours() + " horas seguidas");
        }
        var entidad = entidades.porId(representante.getIdEntidad())
                .filter(e -> e.vigenteEn(peticion.fecha()))
                .orElseThrow(() -> new ReglaNegocioException("CONVENIO_NO_VIGENTE",
                        "El convenio de tu entidad no está vigente en esa fecha"));
        if (entidad.suspendidaEn(LocalDateTime.now(reloj).toLocalDate())) {
            throw new AccesoDenegadoException("ENTIDAD_SUSPENDIDA", "La administración ha suspendido las reservas "
                    + "institucionales de tu entidad hasta el " + entidad.suspendidaHasta().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        Instalacion instalacion = instalacionService.activa(peticion.idInstalacion());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime inicio = LocalDateTime.of(peticion.fecha(), peticion.horaInicio());
        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("RESERVA_EN_PASADO", "No se puede reservar una franja que ya ha empezado");
        }
        if (inicio.isBefore(ahora.plus(antelacionMinima))) {
            throw new ReglaNegocioException("ANTELACION_MINIMA", "Las reservas institucionales se solicitan con al menos "
                    + antelacionMinima.toHours() + " horas de antelación");
        }

        // Misma fila por instalación que las reservas individuales (sin deadlocks; ver FranjaRepository)
        franjaRepository.bloquearInstalacion(instalacion.getId());
        comprobarFranjasLibres(instalacion, peticion);

        Reserva reserva = Reserva.institucional(representante, instalacion, representante.getIdEntidad(),
                peticion.fecha(), peticion.horaInicio(), peticion.horaFin(), ahora);
        reservas.saveAndFlush(reserva);
        return ReservaDto.from(reserva, ahora);
    }

    @Transactional(readOnly = true)
    public List<ReservaInstitucionalDto> listar(EstadoReserva estado) {
        return consultas.buscar(estado);
    }

    @Transactional
    public ReservaDto autorizar(Integer idAdmin, Integer idReserva) {
        Reserva reserva = pendiente(idReserva);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!reserva.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("RESERVA_INICIADA",
                    "La reserva ya ha empezado: no se puede autorizar");
        }
        reserva.autorizar(idAdmin, ahora);
        reservas.flush();
        return ReservaDto.from(reserva, ahora);
    }

    @Transactional
    public ReservaDto rechazar(Integer idAdmin, Integer idReserva, String motivo) {
        Reserva reserva = pendiente(idReserva);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        reserva.rechazar(idAdmin, motivo.trim(), ahora);
        reservas.flush();
        return ReservaDto.from(reserva, ahora);
    }

    /**
     * La petición debe empezar en el inicio de una franja y terminar en el final de otra, y todas
     * las franjas intermedias deben estar libres (no reservadas, bloqueadas ni fuera de plazo).
     */
    private void comprobarFranjasLibres(Instalacion instalacion, ReservaInstitucionalRequest peticion) {
        List<FranjaDto> afectadas = disponibilidadService.franjas(instalacion, peticion.fecha()).stream()
                .filter(f -> !f.horaInicio().isBefore(peticion.horaInicio())
                        && !f.horaFin().isAfter(peticion.horaFin()))
                .toList();
        boolean encaja = !afectadas.isEmpty()
                && afectadas.getFirst().horaInicio().equals(peticion.horaInicio())
                && afectadas.getLast().horaFin().equals(peticion.horaFin());
        for (int i = 1; encaja && i < afectadas.size(); i++) {
            encaja = afectadas.get(i - 1).horaFin().equals(afectadas.get(i).horaInicio());
        }
        if (!encaja) {
            throw new ReglaNegocioException("FRANJA_NO_VALIDA",
                    "El periodo debe empezar y terminar en el límite de una franja dentro del horario de apertura");
        }
        for (FranjaDto f : afectadas) {
            switch (f.estado()) {
                case RESERVADA -> throw new ConflictoException("FRANJA_OCUPADA",
                        "La franja de las " + f.horaInicio() + " ya está reservada");
                case BLOQUEADA -> throw new ConflictoException("FRANJA_BLOQUEADA",
                        "La instalación está bloqueada a las " + f.horaInicio() + " (" + f.motivo() + ")");
                case PASADA, FUERA_DE_PLAZO -> throw new ReglaNegocioException("ANTELACION_MINIMA",
                        "Hay que reservar con al menos " + politica.antelacionMinimaHoras() + " horas de antelación");
                case LIBRE -> { /* continúa */ }
            }
        }
    }

    private Reserva pendiente(Integer idReserva) {
        Reserva reserva = reservas.findById(idReserva)
                .filter(Reserva::isInstitucional)
                .orElseThrow(() -> new RecursoNoEncontradoException("RESERVA_NO_ENCONTRADA",
                        "La reserva institucional no existe"));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ConflictoException("ESTADO_NO_VALIDO",
                    "La reserva ya está resuelta (" + reserva.getEstado() + ")");
        }
        return reserva;
    }
}
