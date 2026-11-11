package es.ual.sadual.controller;

import es.ual.sadual.dto.InformeDto;
import es.ual.sadual.service.InformeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** P9 · Informes semanales y mensuales del Servicio de Deportes (solo ADMIN). */
@RestController
@RequestMapping("/api/admin/informes")
public class AdminInformeController {

    private final InformeService servicio;

    public AdminInformeController(InformeService servicio) {
        this.servicio = servicio;
    }

    /** periodo = SEMANAL (lunes a domingo de la semana de «fecha») o MENSUAL (mes natural). */
    @GetMapping
    public InformeDto generar(@RequestParam(defaultValue = "SEMANAL") String periodo,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.generar(periodo, fecha);
    }
}
