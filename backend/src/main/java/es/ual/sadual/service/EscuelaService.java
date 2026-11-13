package es.ual.sadual.service;

import es.ual.sadual.dto.ContactoMenorRequest;
import es.ual.sadual.dto.EscuelaDto;
import es.ual.sadual.dto.EscuelaRequest;
import es.ual.sadual.dto.InscritoEscuelaDto;
import es.ual.sadual.dto.MenorDto;
import es.ual.sadual.dto.MenorRequest;
import es.ual.sadual.dto.ResultadoEscuelaDto;
import es.ual.sadual.dto.SesionEscuelaDto;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.EscuelaRepository;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Escuelas infantil-juveniles (Variación 7).
 * <ul>
 *   <li>P19 · La administración crea la escuela con un horario semanal fijo; se generan los bloqueos
 *       ESCUELA de toda la temporada (fn_generar_bloqueos_escuela). Las sesiones que caen en un bloqueo
 *       previo (festivo...) se omiten y no se recuperan (P42).</li>
 *   <li>P41 · No se crea la escuela si hay reservas confirmadas en sus sesiones (ni pendientes, ❓P55): la
 *       administración debe resolver antes la planificación.</li>
 *   <li>❓P56 · La administración puede cancelar una escuela: se liberan sus sesiones futuras y los inscritos
 *       pasan a baja. El horario no se modifica una vez creada.</li>
 *   <li>P17/P39 · Inscriben menores los tutores registrados como externos y cualquier miembro de la
 *       comunidad universitaria que sea tutor legal.</li>
 *   <li>P22/P44/P45 · Sin consentimiento RGPD no se registra al menor; se guardan su fecha, el teléfono del
 *       tutor y un contacto de emergencia. Si el tutor retira el consentimiento, se eliminan los datos.</li>
 *   <li>P20 · La edad se calcula a la fecha de inscripción; tramo y plazas los garantiza la BD.</li>
 * </ul>
 */
@Service
public class EscuelaService {

    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private final EscuelaRepository escuelas;
    private final UsuarioRepository usuarios;
    private final InstalacionService instalacionService;
    private final FranjaRepository franjaRepository;
    private final Set<String> perfilesTutor;
    private final Clock reloj;

    public EscuelaService(EscuelaRepository escuelas, UsuarioRepository usuarios, InstalacionService instalacionService,
                          FranjaRepository franjaRepository,
                          @Value("${sadual.escuelas.perfiles-tutor:ALUMNADO,PDI,PAS,EXTERNO}") Set<String> perfilesTutor,
                          Clock reloj) {
        this.escuelas = escuelas;
        this.usuarios = usuarios;
        this.instalacionService = instalacionService;
        this.franjaRepository = franjaRepository;
        this.perfilesTutor = Set.copyOf(perfilesTutor);
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- administración

    @Transactional
    public ResultadoEscuelaDto crear(Integer idAdmin, EscuelaRequest peticion) {
        if (peticion.edadMax() < peticion.edadMin()) {
            throw new ReglaNegocioException("EDADES_NO_VALIDAS", "La edad máxima no puede ser menor que la mínima");
        }
        if (peticion.fechaFin().isBefore(peticion.fechaInicio())) {
            throw new ReglaNegocioException("FECHAS_NO_VALIDAS", "La temporada debe terminar después de empezar");
        }
        validarHorario(peticion.horario());
        instalacionService.activa(peticion.idInstalacion());
        franjaRepository.bloquearInstalacion(peticion.idInstalacion());

        Integer id = escuelas.insertar(peticion, resumenHorario(peticion.horario()));
        int activas = escuelas.reservasAfectadas(id);
        if (activas > 0) {
            // La transacción se deshace: la escuela no llega a crearse
            throw new ConflictoException("ESCUELA_CON_RESERVAS", "Hay " + activas + " reserva(s) en las sesiones de la "
                    + "escuela. Resuelve antes la planificación (cancélalas, resuelve las pendientes o cambia el horario)");
        }
        int reservasAfectadas = 0;
        int creadas = escuelas.generarBloqueos(id, idAdmin);
        int posibles = sesionesEnTemporada(peticion);
        return new ResultadoEscuelaDto(buscar(id), creadas, posibles - creadas, reservasAfectadas);
    }

    @Transactional(readOnly = true)
    public List<EscuelaDto> todas() {
        return escuelas.todas();
    }

    /** ❓P56 · Cancelación de una escuela por la administración. Devuelve la escuela y cuántos inscritos pasan a baja. */
    @Transactional
    public ResultadoEscuelaDto cancelar(Integer idEscuela) {
        EscuelaDto escuela = buscar(idEscuela);
        if ("CANCELADA".equals(escuela.estado())) {
            throw new ConflictoException("ESCUELA_CANCELADA", "La escuela ya está cancelada");
        }
        franjaRepository.bloquearInstalacion(escuela.idInstalacion());
        int bajas = escuelas.cancelar(idEscuela, LocalDateTime.now(reloj));
        return new ResultadoEscuelaDto(buscar(idEscuela), 0, 0, bajas);
    }

    @Transactional(readOnly = true)
    public List<InscritoEscuelaDto> inscritos(Integer idEscuela) {
        buscar(idEscuela);
        return escuelas.inscritos(idEscuela);
    }

    // ---------------------------------------------------------------- tutores

    @Transactional(readOnly = true)
    public List<EscuelaDto> vigentes() {
        return escuelas.vigentes(LocalDate.now(reloj));
    }

    @Transactional
    public MenorDto registrarMenor(Integer idTutor, MenorRequest peticion) {
        tutor(idTutor);
        if (!Boolean.TRUE.equals(peticion.consentimientoRgpd())) {
            throw new ReglaNegocioException("CONSENTIMIENTO_OBLIGATORIO",
                    "Es necesario el consentimiento del tutor para tratar los datos del menor");
        }
        Integer id = escuelas.insertarMenor(idTutor, peticion.nombre().trim(), peticion.apellidos().trim(),
                peticion.fechaNacimiento(), LocalDateTime.now(reloj), peticion.telefonoTutor().trim(),
                peticion.contactoEmergenciaNombre().trim(), peticion.contactoEmergenciaTelefono().trim());
        return escuelas.menoresDelTutor(idTutor).stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<MenorDto> menores(Integer idTutor) {
        return escuelas.menoresDelTutor(idTutor);
    }

    @Transactional
    public MenorDto inscribir(Integer idTutor, Integer idEscuela, Integer idMenor) {
        tutor(idTutor);
        menorDelTutor(idTutor, idMenor);
        EscuelaDto escuela = buscar(idEscuela);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if ("CANCELADA".equals(escuela.estado())) {
            throw new ConflictoException("ESCUELA_CANCELADA", "La escuela está cancelada");
        }
        if (escuela.fechaFin().isBefore(ahora.toLocalDate())) {
            throw new ReglaNegocioException("TEMPORADA_FINALIZADA", "La temporada de esta escuela ya ha terminado");
        }
        if (escuelas.inscribir(idEscuela, idMenor, ahora) == 0) {
            throw new ConflictoException("YA_INSCRITO", "El menor ya está inscrito en esta escuela");
        }
        return menor(idTutor, idMenor);
    }

    /** ❓P52 · El tutor corrige el teléfono y el contacto de emergencia del menor. */
    @Transactional
    public MenorDto actualizarContacto(Integer idTutor, Integer idMenor, ContactoMenorRequest p) {
        menorDelTutor(idTutor, idMenor);
        escuelas.actualizarContacto(idMenor, p.telefonoTutor().trim(), p.contactoEmergenciaNombre().trim(),
                p.contactoEmergenciaTelefono().trim());
        return menor(idTutor, idMenor);
    }

    /** P45 · El tutor retira el consentimiento: se eliminan los datos del menor y sus inscripciones. */
    @Transactional
    public void retirarConsentimiento(Integer idTutor, Integer idMenor) {
        menorDelTutor(idTutor, idMenor);
        escuelas.eliminarMenor(idMenor);
    }

    @Transactional
    public MenorDto darDeBaja(Integer idTutor, Integer idEscuela, Integer idMenor) {
        menorDelTutor(idTutor, idMenor);
        if (escuelas.darDeBaja(idEscuela, idMenor) == 0) {
            throw new ConflictoException("NO_INSCRITO", "El menor no tiene una inscripción activa en esta escuela");
        }
        return menor(idTutor, idMenor);
    }

    // ---------------------------------------------------------------- apoyo

    private Usuario tutor(Integer idUsuario) {
        Usuario u = usuarios.findById(idUsuario).filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_NO_VALIDO", "Tu usuario no está activo"));
        if (!perfilesTutor.contains(u.getRol().getCodigo())) {
            throw new AccesoDenegadoException("PERFIL_NO_AUTORIZADO",
                    "La administración no gestiona menores como tutora");
        }
        return u;
    }

    private void menorDelTutor(Integer idTutor, Integer idMenor) {
        escuelas.menor(idMenor)
                .filter(m -> m.idTutor().equals(idTutor))
                .orElseThrow(() -> new RecursoNoEncontradoException("MENOR_NO_ENCONTRADO",
                        "El menor no existe o no está a tu cargo"));
    }

    private MenorDto menor(Integer idTutor, Integer idMenor) {
        return escuelas.menoresDelTutor(idTutor).stream().filter(m -> m.id().equals(idMenor)).findFirst().orElseThrow();
    }

    private EscuelaDto buscar(Integer idEscuela) {
        return escuelas.buscar(idEscuela).orElseThrow(() ->
                new RecursoNoEncontradoException("ESCUELA_NO_ENCONTRADA", "La escuela no existe"));
    }

    /** Las sesiones de un mismo día no pueden solaparse (generarían dos bloqueos que chocan). */
    static void validarHorario(List<SesionEscuelaDto> horario) {
        List<SesionEscuelaDto> ordenado = horario.stream()
                .sorted(Comparator.comparing(SesionEscuelaDto::diaSemana).thenComparing(SesionEscuelaDto::horaInicio))
                .toList();
        for (int i = 0; i < ordenado.size(); i++) {
            SesionEscuelaDto s = ordenado.get(i);
            if (!s.horaFin().isAfter(s.horaInicio())) {
                throw new ReglaNegocioException("HORARIO_NO_VALIDO", "Cada sesión debe terminar después de empezar");
            }
            if (i > 0) {
                SesionEscuelaDto anterior = ordenado.get(i - 1);
                if (anterior.diaSemana().equals(s.diaSemana()) && s.horaInicio().isBefore(anterior.horaFin())) {
                    throw new ReglaNegocioException("HORARIO_NO_VALIDO", "Hay sesiones solapadas el mismo día");
                }
            }
        }
    }

    /** Resumen legible para escuela.dias_horario, p. ej. "lun 17:00-18:00, mié 17:00-18:00". */
    static String resumenHorario(List<SesionEscuelaDto> horario) {
        String resumen = horario.stream()
                .sorted(Comparator.comparing(SesionEscuelaDto::diaSemana).thenComparing(SesionEscuelaDto::horaInicio))
                .map(s -> DayOfWeek.of(s.diaSemana()).getDisplayName(TextStyle.SHORT, ES) + " "
                        + s.horaInicio() + "-" + s.horaFin())
                .collect(Collectors.joining(", "));
        return resumen.length() > 100 ? resumen.substring(0, 100) : resumen;
    }

    static int sesionesEnTemporada(EscuelaRequest p) {
        return (int) p.fechaInicio().datesUntil(p.fechaFin().plusDays(1))
                .mapToLong(d -> p.horario().stream().filter(s -> s.diaSemana() == d.getDayOfWeek().getValue()).count())
                .sum();
    }
}
