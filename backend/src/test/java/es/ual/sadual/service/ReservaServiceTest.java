package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.EstadoFranja;
import es.ual.sadual.dto.FranjaDto;
import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.dto.ReservaRequest;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.Reserva;
import es.ual.sadual.entity.Rol;
import es.ual.sadual.entity.TipoInstalacion;
import es.ual.sadual.entity.Usuario;
import es.ual.sadual.exception.AccesoDenegadoException;
import es.ual.sadual.exception.ConflictoException;
import es.ual.sadual.exception.RecursoNoEncontradoException;
import es.ual.sadual.exception.ReglaNegocioException;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.ReservaRepository;
import es.ual.sadual.repository.SancionRepository;
import es.ual.sadual.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Reglas de negocio de F04/F05 sin base de datos ni Spring. */
class ReservaServiceTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 23, 10, 0);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 27);

    private ReservaRepository reservas;
    private UsuarioRepository usuarios;
    private SancionRepository sanciones;
    private FranjaRepository franjaRepository;
    private InstalacionService instalacionService;
    private DisponibilidadService disponibilidad;
    private ReservaService servicio;

    private Usuario alumna;
    private Instalacion padel;

    @BeforeEach
    void preparar() {
        reservas = mock(ReservaRepository.class);
        usuarios = mock(UsuarioRepository.class);
        sanciones = mock(SancionRepository.class);
        franjaRepository = mock(FranjaRepository.class);
        instalacionService = mock(InstalacionService.class);
        disponibilidad = mock(DisponibilidadService.class);
        Clock reloj = Clock.fixed(AHORA.atZone(MADRID).toInstant(), MADRID);
        PoliticaReservas politica = new PoliticaReservas(Set.of("ALUMNADO", "PDI", "PAS", "ADMIN"), Duration.ofHours(3));
        servicio = new ReservaService(reservas, usuarios, sanciones, instalacionService, disponibilidad,
                franjaRepository, politica, reloj);

        alumna = new Usuario(new Rol("ALUMNADO", "Alumnado"), "Lucía", "Martín Ortega", "alumna.demo@ual.es", "x");
        ReflectionTestUtils.setField(alumna, "id", 2);
        padel = new Instalacion(new TipoInstalacion("Pádel"), "Pista de pádel 1", (short) 4, (short) 90);
        ReflectionTestUtils.setField(padel, "id", 1);

        when(usuarios.findById(2)).thenReturn(Optional.of(alumna));
        when(instalacionService.activa(1)).thenReturn(padel);
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sanciones.vigenteEn(any(), any())).thenReturn(Optional.empty());
    }

    private void franjasDelDia(FranjaDto... franjas) {
        when(disponibilidad.franjas(padel, MARTES)).thenReturn(List.of(franjas));
    }

    private static FranjaDto franja(String inicio, String fin, EstadoFranja estado) {
        return new FranjaDto(LocalTime.parse(inicio), LocalTime.parse(fin), estado,
                estado == EstadoFranja.BLOQUEADA ? "DOCENCIA" : null);
    }

    @Nested
    @DisplayName("Crear reserva (F04)")
    class Crear {

        @Test
        void reserva_una_franja_libre_y_calcula_la_hora_de_fin() {
            franjasDelDia(franja("09:30", "11:00", EstadoFranja.RESERVADA), franja("11:00", "12:30", EstadoFranja.LIBRE));

            ReservaDto r = servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(11, 0)));

            assertThat(r.horaInicio()).isEqualTo(LocalTime.of(11, 0));
            assertThat(r.horaFin()).isEqualTo(LocalTime.of(12, 30));
            assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(r.cancelable()).isTrue();
            verify(reservas).saveAndFlush(any(Reserva.class));
        }

        @Test
        void bloquea_la_instalacion_antes_de_comprobar_la_franja_y_de_insertar() {
            franjasDelDia(franja("11:00", "12:30", EstadoFranja.LIBRE));

            servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(11, 0)));

            var orden = inOrder(franjaRepository, disponibilidad, reservas);
            orden.verify(franjaRepository).bloquearInstalacion(1);
            orden.verify(disponibilidad).franjas(padel, MARTES);
            orden.verify(reservas).saveAndFlush(any(Reserva.class));
        }

        @Test
        void no_bloquea_si_la_peticion_ya_es_invalida() {
            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, AHORA.toLocalDate(), LocalTime.of(9, 0))))
                    .isInstanceOf(ReglaNegocioException.class);
            verifyNoInteractions(franjaRepository);
        }

        @Test
        void rechaza_una_franja_ya_reservada() {
            franjasDelDia(franja("09:30", "11:00", EstadoFranja.RESERVADA));

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(9, 30))))
                    .isInstanceOf(ConflictoException.class)
                    .extracting("codigo").isEqualTo("FRANJA_OCUPADA");
            verify(reservas, never()).saveAndFlush(any());
        }

        @Test
        void rechaza_una_franja_bloqueada() {
            franjasDelDia(franja("09:30", "11:00", EstadoFranja.BLOQUEADA));

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(9, 30))))
                    .isInstanceOf(ConflictoException.class)
                    .extracting("codigo").isEqualTo("FRANJA_BLOQUEADA");
            verify(reservas, never()).saveAndFlush(any());
        }

        @Test
        void rechaza_una_hora_que_no_es_inicio_de_franja() {
            franjasDelDia(franja("08:00", "09:30", EstadoFranja.LIBRE), franja("09:30", "11:00", EstadoFranja.LIBRE));

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(9, 0))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("FRANJA_NO_VALIDA");
        }

        @Test
        void rechaza_un_dia_en_que_la_instalacion_no_abre() {
            franjasDelDia();

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(10, 0))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("FRANJA_NO_VALIDA");
        }

        @Test
        void rechaza_reservar_en_el_pasado_sin_consultar_disponibilidad() {
            LocalDate hoy = AHORA.toLocalDate();

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, hoy, LocalTime.of(9, 30))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_EN_PASADO");
            verifyNoInteractions(disponibilidad);
        }

        @Test
        void rechaza_una_franja_que_empieza_justo_ahora() {
            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, AHORA.toLocalDate(), AHORA.toLocalTime())))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_EN_PASADO");
        }

        @Test
        void P2_exige_tres_horas_de_antelacion() {
            LocalDate hoy = AHORA.toLocalDate(); // ahora son las 10:00

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, hoy, LocalTime.of(12, 59))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("ANTELACION_MINIMA");
            verifyNoInteractions(disponibilidad);
        }

        @Test
        void P2_con_exactamente_tres_horas_se_puede_reservar() {
            LocalDate hoy = AHORA.toLocalDate();
            when(disponibilidad.franjas(padel, hoy)).thenReturn(List.of(franja("13:00", "14:30", EstadoFranja.LIBRE)));

            assertThat(servicio.crear(2, new ReservaRequest(1, hoy, LocalTime.of(13, 0))).estado())
                    .isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        void P1_solo_una_reserva_por_dia() {
            franjasDelDia(franja("11:00", "12:30", EstadoFranja.LIBRE));
            when(reservas.existsByUsuarioIdAndFechaAndEstadoInAndIdEntidadIsNull(eq(2), eq(MARTES), anyCollection()))
                    .thenReturn(true);

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(11, 0))))
                    .isInstanceOf(ConflictoException.class)
                    .extracting("codigo").isEqualTo("LIMITE_DIARIO");
            verify(reservas, never()).saveAndFlush(any());
        }

        @Test
        void P3_un_usuario_sancionado_no_puede_reservar_esa_semana() {
            when(sanciones.vigenteEn(2, AHORA.toLocalDate())).thenReturn(Optional.of(LocalDate.of(2026, 10, 25)));

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(11, 0))))
                    .isInstanceOf(AccesoDenegadoException.class)
                    .hasMessageContaining("26/10/2026")
                    .extracting("codigo").isEqualTo("USUARIO_SANCIONADO");
            verifyNoInteractions(disponibilidad);
        }

        @Test
        void P4_un_usuario_externo_no_hace_reservas_individuales() {
            Usuario externo = new Usuario(new Rol("EXTERNO", "Usuario externo"), "Pablo", "Romero", "e@correo.test", "x");
            ReflectionTestUtils.setField(externo, "id", 5);
            when(usuarios.findById(5)).thenReturn(Optional.of(externo));

            assertThatThrownBy(() -> servicio.crear(5, new ReservaRequest(1, MARTES, LocalTime.of(11, 0))))
                    .isInstanceOf(AccesoDenegadoException.class)
                    .extracting("codigo").isEqualTo("PERFIL_NO_AUTORIZADO");
            verifyNoInteractions(disponibilidad);
        }

        @Test
        void un_usuario_desactivado_no_puede_reservar() {
            alumna.setActivo(false);

            assertThatThrownBy(() -> servicio.crear(2, new ReservaRequest(1, MARTES, LocalTime.of(11, 0))))
                    .isInstanceOf(AccesoDenegadoException.class);
            verifyNoInteractions(disponibilidad);
        }
    }

    @Nested
    @DisplayName("Cancelar reserva (F05)")
    class Cancelar {

        private Reserva reservaDeAlumna(LocalDate fecha, LocalTime inicio) {
            Reserva r = Reserva.confirmada(alumna, padel, fecha, inicio, inicio.plusMinutes(90), AHORA.minusDays(1));
            ReflectionTestUtils.setField(r, "id", 10);
            when(reservas.findById(10)).thenReturn(Optional.of(r));
            return r;
        }

        @Test
        void cancela_una_reserva_propia_futura() {
            Reserva r = reservaDeAlumna(MARTES, LocalTime.of(9, 30));

            ReservaDto dto = servicio.cancelar(2, false, 10);

            assertThat(dto.estado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(dto.cancelable()).isFalse();
            assertThat(r.getFechaCancelacion()).isEqualTo(AHORA);
        }

        @Test
        void no_permite_cancelar_reservas_ajenas() {
            reservaDeAlumna(MARTES, LocalTime.of(9, 30));

            assertThatThrownBy(() -> servicio.cancelar(3, false, 10))
                    .isInstanceOf(AccesoDenegadoException.class)
                    .extracting("codigo").isEqualTo("RESERVA_AJENA");
        }

        @Test
        void el_administrador_puede_cancelar_cualquier_reserva() {
            reservaDeAlumna(MARTES, LocalTime.of(9, 30));

            assertThat(servicio.cancelar(1, true, 10).estado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        void no_cancela_dos_veces() {
            Reserva r = reservaDeAlumna(MARTES, LocalTime.of(9, 30));
            r.cancelar(AHORA.minusHours(1));

            assertThatThrownBy(() -> servicio.cancelar(2, false, 10))
                    .isInstanceOf(ConflictoException.class)
                    .extracting("codigo").isEqualTo("RESERVA_NO_CANCELABLE");
        }

        @Test
        void no_cancela_una_reserva_ya_empezada() {
            reservaDeAlumna(AHORA.toLocalDate(), LocalTime.of(9, 30));

            assertThatThrownBy(() -> servicio.cancelar(2, false, 10))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_INICIADA");
        }

        @Test
        void P3_marcar_no_presentado_solo_cuando_la_reserva_ya_ha_empezado() {
            reservaDeAlumna(MARTES, LocalTime.of(9, 30));
            assertThatThrownBy(() -> servicio.marcarNoPresentado(10))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_NO_INICIADA");
        }

        @Test
        void P3_marca_no_presentado_una_reserva_confirmada_ya_empezada() {
            reservaDeAlumna(AHORA.toLocalDate(), LocalTime.of(8, 0));
            assertThat(servicio.marcarNoPresentado(10).estado()).isEqualTo(EstadoReserva.NO_PRESENTADO);
        }

        @Test
        void una_reserva_inexistente_da_404() {
            when(reservas.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.cancelar(2, false, 99))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }
}
