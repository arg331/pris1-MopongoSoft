package es.ual.sadual.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("F02 · Catálogo y F03 · Disponibilidad")
class CatalogoDisponibilidadIT extends AbstractIntegracionIT {

    private String auth() {
        return bearer(token("alumna.demo@ual.es"));
    }

    // ---------- F02 ----------

    @Test
    void el_catalogo_muestra_las_9_instalaciones_activas_con_tipo_ubicacion_y_aforo() throws Exception {
        mvc.perform(get("/api/instalaciones").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0].tipo").exists())
                .andExpect(jsonPath("$[0].ubicacion").exists())
                .andExpect(jsonPath("$[0].aforo").exists());
    }

    @Test
    void el_catalogo_se_filtra_por_tipo() throws Exception {
        Short idPadel = jdbc.queryForObject("SELECT id_tipo FROM tipo_instalacion WHERE nombre = 'Pádel'", Short.class);

        mvc.perform(get("/api/instalaciones").param("tipo", String.valueOf(idPadel)).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tipo", everyItem(is("Pádel"))));
    }

    @Test
    void una_instalacion_desactivada_no_aparece_ni_se_puede_consultar() throws Exception {
        jdbc.update("UPDATE instalacion SET activa = FALSE WHERE id_instalacion = ?", INST_SALA);

        mvc.perform(get("/api/instalaciones").header("Authorization", auth()))
                .andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_SALA).param("fecha", "2026-10-27")
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("INSTALACION_NO_ENCONTRADA"));
    }

    @Test
    void los_tipos_alimentan_el_filtro() throws Exception {
        mvc.perform(get("/api/tipos-instalacion").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    // ---------- F03 ----------

    @Test
    void la_disponibilidad_marca_las_franjas_reservadas() throws Exception {
        // Pista de pádel 1, martes 27/10: franjas de 90 min de 08:00 a 21:30; 09:30 está reservada en el seed
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_PADEL_1).param("fecha", "2026-10-27")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionFranjaMin").value(90))
                .andExpect(jsonPath("$.franjas", hasSize(9)))
                .andExpect(jsonPath("$.franjas[0].horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$.franjas[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$.franjas[1].horaInicio").value("09:30:00"))
                .andExpect(jsonPath("$.franjas[1].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.franjas[8].horaFin").value("21:30:00"));
    }

    @Test
    void la_disponibilidad_incluye_los_bloqueos_con_su_motivo() throws Exception {
        // Pabellón, lunes 26/10: bloqueado por docencia de 08:00 a 14:00
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_PABELLON).param("fecha", "2026-10-26")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas", hasSize(14)))
                .andExpect(jsonPath("$.franjas[0].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.franjas[0].motivo").value("DOCENCIA"))
                .andExpect(jsonPath("$.franjas[5].horaInicio").value("13:00:00"))
                .andExpect(jsonPath("$.franjas[5].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.franjas[6].horaInicio").value("14:00:00"))
                .andExpect(jsonPath("$.franjas[6].estado").value("LIBRE"));
    }

    @Test
    void el_sabado_solo_hay_horario_de_mañana_y_el_domingo_esta_cerrado() throws Exception {
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_TENIS_1).param("fecha", "2026-10-31")
                        .header("Authorization", auth()))
                .andExpect(jsonPath("$.franjas", hasSize(5)));
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_TENIS_1).param("fecha", "2026-11-01")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas", hasSize(0)));
    }

    @Test
    void hoy_se_distinguen_las_franjas_pasadas_y_las_de_menos_de_tres_horas() throws Exception {
        // Reloj de pruebas: 23/10/2026 10:00 · P2: antelación mínima de 3 horas
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_TENIS_1).param("fecha", "2026-10-23")
                        .header("Authorization", auth()))
                .andExpect(jsonPath("$.antelacionMinimaHoras").value(3))
                .andExpect(jsonPath("$.franjas[1].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.franjas[1].estado").value("PASADA"))
                .andExpect(jsonPath("$.franjas[2].estado").value("PASADA"))
                .andExpect(jsonPath("$.franjas[3].horaInicio").value("11:00:00"))
                .andExpect(jsonPath("$.franjas[3].estado").value("FUERA_DE_PLAZO"))
                .andExpect(jsonPath("$.franjas[4].estado").value("FUERA_DE_PLAZO"))
                .andExpect(jsonPath("$.franjas[5].horaInicio").value("13:00:00"))
                .andExpect(jsonPath("$.franjas[5].estado").value("LIBRE"));
    }

    @Test
    void errores_de_consulta() throws Exception {
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", 999).param("fecha", "2026-10-27")
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_TENIS_1).param("fecha", "27/10/2026")
                        .header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", INST_TENIS_1)
                        .header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
        mvc.perform(get("/api/instalaciones/{id}/disponibilidad", "abc").param("fecha", "2026-10-27")
                        .header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }
}
