package es.ual.sadual.service;

import es.ual.sadual.config.PoliticaReservas;
import es.ual.sadual.dto.EstadoFranja;
import es.ual.sadual.dto.FranjaDto;
import es.ual.sadual.entity.Instalacion;
import es.ual.sadual.entity.TipoInstalacion;
import es.ual.sadual.repository.FranjaRepository;
import es.ual.sadual.repository.FranjaRepository.BloqueoBd;
import es.ual.sadual.repository.FranjaRepository.FranjaBd;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** F03: cómo se etiqueta cada franja (libre, reservada, bloqueada, pasada). */
class DisponibilidadServiceTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 23, 10, 0);
    private static final List<BloqueoBd> SIN_BLOQUEOS = List.of();
    private static final Duration TRES_HORAS = Duration.ofHours(3);

    private static FranjaBd franja(String ini, String fin, boolean disponible) {
        return new FranjaBd(LocalTime.parse(ini), LocalTime.parse(fin), disponible);
    }

    private static BloqueoBd docencia(String ini, String fin) {
        return new BloqueoBd(LUNES.atTime(LocalTime.parse(ini)), LUNES.atTime(LocalTime.parse(fin)), "DOCENCIA");
    }

    @Test
    void franja_libre_en_el_futuro() {
        FranjaDto f = DisponibilidadService.clasificar(franja("10:00", "11:00", true), LUNES, SIN_BLOQUEOS, AHORA, TRES_HORAS);
        assertThat(f.estado()).isEqualTo(EstadoFranja.LIBRE);
        assertThat(f.motivo()).isNull();
    }

    @Test
    void franja_ocupada_sin_bloqueo_es_reservada() {
        FranjaDto f = DisponibilidadService.clasificar(franja("10:00", "11:00", false), LUNES, SIN_BLOQUEOS, AHORA, TRES_HORAS);
        assertThat(f.estado()).isEqualTo(EstadoFranja.RESERVADA);
    }

    @Test
    void franja_que_solapa_con_un_bloqueo_es_bloqueada_con_su_motivo() {
        FranjaDto f = DisponibilidadService.clasificar(franja("13:00", "14:00", false), LUNES,
                List.of(docencia("08:00", "14:00")), AHORA, TRES_HORAS);
        assertThat(f.estado()).isEqualTo(EstadoFranja.BLOQUEADA);
        assertThat(f.motivo()).isEqualTo("DOCENCIA");
    }

    @Test
    void franja_contigua_a_un_bloqueo_sigue_libre() {
        FranjaDto f = DisponibilidadService.clasificar(franja("14:00", "15:00", true), LUNES,
                List.of(docencia("08:00", "14:00")), AHORA, TRES_HORAS);
        assertThat(f.estado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void franja_libre_que_ya_empezo_es_pasada() {
        LocalDate hoy = AHORA.toLocalDate();
        assertThat(DisponibilidadService.clasificar(franja("09:00", "10:00", true), hoy, SIN_BLOQUEOS, AHORA, TRES_HORAS).estado())
                .isEqualTo(EstadoFranja.PASADA);
        assertThat(DisponibilidadService.clasificar(franja("10:00", "11:00", true), hoy, SIN_BLOQUEOS, AHORA, TRES_HORAS).estado())
                .isEqualTo(EstadoFranja.PASADA);
    }

    @Test
    void P2_franja_libre_a_menos_de_tres_horas_esta_fuera_de_plazo() {
        LocalDate hoy = AHORA.toLocalDate(); // 10:00
        assertThat(DisponibilidadService.clasificar(franja("11:00", "12:00", true), hoy, SIN_BLOQUEOS, AHORA, TRES_HORAS).estado())
                .isEqualTo(EstadoFranja.FUERA_DE_PLAZO);
        assertThat(DisponibilidadService.clasificar(franja("12:00", "13:00", true), hoy, SIN_BLOQUEOS, AHORA, TRES_HORAS).estado())
                .isEqualTo(EstadoFranja.FUERA_DE_PLAZO);
        assertThat(DisponibilidadService.clasificar(franja("13:00", "14:00", true), hoy, SIN_BLOQUEOS, AHORA, TRES_HORAS).estado())
                .isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void dia_sin_horario_devuelve_lista_vacia_sin_consultar_bloqueos() {
        FranjaRepository repo = mock(FranjaRepository.class);
        when(repo.franjas(anyInt(), any())).thenReturn(List.of());
        Instalacion tenis = new Instalacion(new TipoInstalacion("Tenis"), "Pista de tenis 1", (short) 4, (short) 60);
        ReflectionTestUtils.setField(tenis, "id", 3);
        DisponibilidadService servicio = new DisponibilidadService(mock(InstalacionService.class), repo,
                new PoliticaReservas(Set.of("ALUMNADO"), TRES_HORAS),
                Clock.fixed(AHORA.atZone(ZoneId.of("Europe/Madrid")).toInstant(), ZoneId.of("Europe/Madrid")));

        assertThat(servicio.franjas(tenis, LocalDate.of(2026, 11, 1))).isEmpty();
        verify(repo, never()).bloqueosDelDia(anyInt(), any());
    }
}
