package es.ual.sadual.controller;

import es.ual.sadual.dto.ReservaDto;
import es.ual.sadual.service.ReservaService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones del Servicio de Deportes sobre reservas. /api/admin/** exige perfil ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservaController {

    private final ReservaService reservaService;

    public AdminReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /** P3 · Marca que el usuario no se presentó; la BD le aplica la sanción de esa semana. */
    @PatchMapping("/{id}/no-presentado")
    public ReservaDto noPresentado(@PathVariable Integer id) {
        return reservaService.marcarNoPresentado(id);
    }
}
