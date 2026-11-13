package es.ual.sadual.service;

import es.ual.sadual.dto.HorarioDto;
import es.ual.sadual.dto.InstalacionAdminDto;
import es.ual.sadual.dto.InstalacionRequest;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.TipoInstalacion;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.AdminRepository;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.InstalacionRepository;
import es.ual.sadual.repository.TipoInstalacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * F06 · Panel de instalaciones (núcleo común obligatorio: alta y baja de instalaciones).
 * <ul>
 *   <li>P12 · La duración de franja se configura por instalación. Cambiarla no altera las reservas
 *       ya hechas (interpretación documentada en requisitos-cliente.md).</li>
 *   <li>P11 · Sin horario explícito, una instalación nueva abre L–V 08:00–22:00 y sábados 09:00–14:00.</li>
 *   <li>La baja es una desactivación (la instalación deja de mostrarse y de poder reservarse, pero se
 *       conserva su histórico). Con reservas futuras activas no se permite (❓P47).</li>
 * </ul>
 */
@Service
public class AdminInstalacionService {

    /** P11 · Horario general validado por el cliente. */
    static final List<HorarioDto> HORARIO_GENERAL = horarioGeneral();

    private final InstalacionRepository instalaciones;
    private final TipoInstalacionRepository tipos;
    private final AdminRepository admin;
    private final FranjaRepository franjaRepository;
    private final Clock reloj;

    public AdminInstalacionService(InstalacionRepository instalaciones, TipoInstalacionRepository tipos,
                                   AdminRepository admin, FranjaRepository franjaRepository, Clock reloj) {
        this.instalaciones = instalaciones;
        this.tipos = tipos;
        this.admin = admin;
        this.franjaRepository = franjaRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<InstalacionAdminDto> todas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return instalaciones.findAll().stream()
                .sorted(Comparator.comparing((Instalacion i) -> i.getTipo().getNombre()).thenComparing(Instalacion::getNombre))
                .map(i -> dto(i, ahora))
                .toList();
    }

    @Transactional
    public InstalacionAdminDto crear(InstalacionRequest p) {
        List<HorarioDto> horario = p.horario() == null || p.horario().isEmpty() ? HORARIO_GENERAL : p.horario();
        validarHorario(horario);
        Instalacion nueva = new Instalacion(tipo(p.idTipo()), p.nombre().trim(), p.aforo().shortValue(),
                p.duracionFranjaMin().shortValue());
        nueva.actualizar(tipo(p.idTipo()), p.nombre().trim(), blancoANulo(p.ubicacion()), p.aforo().shortValue(),
                p.duracionFranjaMin().shortValue(), blancoANulo(p.descripcion()));
        instalaciones.saveAndFlush(nueva);
        admin.reemplazarHorario(nueva.getId(), horario);
        return dto(nueva, LocalDateTime.now(reloj));
    }

    @Transactional
    public InstalacionAdminDto editar(Integer id, InstalacionRequest p) {
        Instalacion instalacion = buscar(id);
        franjaRepository.bloquearInstalacion(id);   // sin reservas a medio crear mientras cambia
        instalacion.actualizar(tipo(p.idTipo()), p.nombre().trim(), blancoANulo(p.ubicacion()), p.aforo().shortValue(),
                p.duracionFranjaMin().shortValue(), blancoANulo(p.descripcion()));
        instalaciones.flush();
        if (p.horario() != null) {
            if (p.horario().isEmpty()) {
                throw new ReglaNegocioException("HORARIO_NO_VALIDO",
                        "Indica al menos un tramo de apertura; para dejar de ofrecerla, desactívala");
            }
            validarHorario(p.horario());
            admin.reemplazarHorario(id, p.horario());
        }
        return dto(instalacion, LocalDateTime.now(reloj));
    }

    @Transactional
    public InstalacionAdminDto desactivar(Integer id) {
        Instalacion instalacion = buscar(id);
        franjaRepository.bloquearInstalacion(id);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int futuras = admin.reservasFuturas(id, ahora);
        if (futuras > 0) {
            throw new ConflictoException("INSTALACION_CON_RESERVAS", "La instalación tiene " + futuras
                    + " reserva(s) futura(s). Cancélalas o bloquea la instalación antes de darla de baja");
        }
        instalacion.setActiva(false);
        instalaciones.flush();
        return dto(instalacion, ahora);
    }

    @Transactional
    public InstalacionAdminDto activar(Integer id) {
        Instalacion instalacion = buscar(id);
        instalacion.setActiva(true);
        instalaciones.flush();
        return dto(instalacion, LocalDateTime.now(reloj));
    }

    // ---------------------------------------------------------------- apoyo

    /** Cada tramo debe cerrar después de abrir y los tramos de un mismo día no pueden solaparse. */
    static void validarHorario(List<HorarioDto> horario) {
        List<HorarioDto> ordenado = horario.stream()
                .sorted(Comparator.comparing(HorarioDto::diaSemana).thenComparing(HorarioDto::horaApertura)).toList();
        for (int i = 0; i < ordenado.size(); i++) {
            HorarioDto h = ordenado.get(i);
            if (!h.horaCierre().isAfter(h.horaApertura())) {
                throw new ReglaNegocioException("HORARIO_NO_VALIDO", "Cada tramo debe cerrar después de abrir");
            }
            if (i > 0 && ordenado.get(i - 1).diaSemana().equals(h.diaSemana())
                    && h.horaApertura().isBefore(ordenado.get(i - 1).horaCierre())) {
                throw new ReglaNegocioException("HORARIO_NO_VALIDO", "Hay tramos solapados el mismo día");
            }
        }
    }

    private InstalacionAdminDto dto(Instalacion i, LocalDateTime ahora) {
        return new InstalacionAdminDto(i.getId(), i.getNombre(), i.getTipo().getId(), i.getTipo().getNombre(),
                i.getUbicacion(), i.getAforo(), i.getDuracionFranjaMin(), i.getDescripcion(), i.isActiva(),
                admin.horario(i.getId()), admin.reservasFuturas(i.getId(), ahora));
    }

    private Instalacion buscar(Integer id) {
        return instalaciones.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("INSTALACION_NO_ENCONTRADA", "La instalación no existe"));
    }

    private TipoInstalacion tipo(Short idTipo) {
        return tipos.findById(idTipo).orElseThrow(() ->
                new ReglaNegocioException("TIPO_NO_VALIDO", "El tipo de instalación no existe"));
    }

    private static String blancoANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static List<HorarioDto> horarioGeneral() {
        List<HorarioDto> h = new ArrayList<>();
        for (int dia = 1; dia <= 5; dia++) {
            h.add(new HorarioDto(dia, LocalTime.of(8, 0), LocalTime.of(22, 0)));
        }
        h.add(new HorarioDto(6, LocalTime.of(9, 0), LocalTime.of(14, 0)));
        return List.copyOf(h);
    }
}
