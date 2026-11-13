package es.ual.sadual.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * P9 · Informe del Servicio de Deportes para un periodo semanal o mensual: ocupación por instalación y
 * por franja, uso por perfil, cancelaciones y no presentados. Definiciones de cálculo: ❓P51.
 */
public record InformeDto(String periodo, LocalDate desde, LocalDate hasta,
                         List<OcupacionInstalacion> ocupacionPorInstalacion,
                         List<OcupacionFranja> ocupacionPorFranja,
                         List<UsoPerfil> usoPorPerfil,
                         Cancelaciones cancelaciones,
                         NoPresentados noPresentados) {

    /** porcentaje = horas reservadas ÷ (horas de apertura − horas bloqueadas) × 100. */
    public record OcupacionInstalacion(Integer idInstalacion, String instalacion, String tipo,
                                       BigDecimal horasApertura, BigDecimal horasBloqueadas,
                                       BigDecimal horasReservadas, BigDecimal porcentaje) {
    }

    /** Reservas que empiezan en cada hora del día (todas las instalaciones). */
    public record OcupacionFranja(int hora, int reservas) {
    }

    public record UsoPerfil(String rol, int reservas, int usuarios, BigDecimal horas) {
    }

    public record Cancelaciones(int canceladas, int rechazadas, List<PorInstalacion> porInstalacion) {
    }

    public record PorInstalacion(String instalacion, int cantidad) {
    }

    public record NoPresentados(int total, List<PorUsuario> porUsuario) {
    }

    public record PorUsuario(Integer idUsuario, String usuario, String rol, int veces) {
    }
}
