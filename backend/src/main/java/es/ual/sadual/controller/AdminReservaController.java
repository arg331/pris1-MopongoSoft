package es.ual.sadual.controller;

import es.ual.sadual.dto.ReservaAdminDto;
import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.entity.EstadoReserva;
import es.ual.sadual.service.ConsultaReservasService;
import es.ual.sadual.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Operaciones del Servicio de Deportes sobre reservas. /api/admin/** exige perfil ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservaController {

    private final ReservaService reservaService;
    private final ConsultaReservasService consulta;

    public AdminReservaController(ReservaService reservaService, ConsultaReservasService consulta) {
        this.reservaService = reservaService;
        this.consulta = consulta;
    }

    /** F10 · Consulta general de reservas con filtros (por defecto, de hoy a 30 días vista). */
    @GetMapping
    public List<ReservaAdminDto> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idInstalacion,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String texto) {
        return consulta.buscar(desde, hasta, idInstalacion, estado, texto);
    }

    /** P3 · Marca que el usuario no se presentó; la BD le aplica la sanción de esa semana. */
    @PatchMapping("/{id}/no-presentado")
    public ReservaDto noPresentado(@PathVariable Integer id) {
        return reservaService.marcarNoPresentado(id);
    }
}
