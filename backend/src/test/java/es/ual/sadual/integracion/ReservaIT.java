package es.ual.sadual.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seed: alumna tiene reserva el 27/10 (pádel 1, 09:30), PDI el 27/10 (tenis 1, 18:00),
 * PAS el 28/10 (piscina, 14:00). Reloj de pruebas: viernes 23/10/2026 10:00.
 */
@DisplayName("F04 · Reservar y F05 · Mis reservas y cancelación (reglas P1–P4, P7)")
class ReservaIT extends AbstractIntegracionIT {

    private static final String ALUMNA = "alumna.demo@ual.es";
    private static final String PDI = "pdi.demo@ual.es";
    private static final String PAS = "pas.demo@ual.es";
    private static final String EXTERNO = "externo.demo@correo.test";
    private static final String ADMIN = "admin.deportes@ual.es";

    private ResultActions reservar(String email, int idInstalacion, String fecha, String hora) throws Exception {
        String cuerpo = "{\"idInstalacion\":" + idInstalacion + ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + hora + "\"}";
        return mvc.perform(post("/api/reservas").header("Authorization", bearer(token(email)))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cancelar(String email, int idReserva) throws Exception {
        return mvc.perform(patch("/api/reservas/{id}/cancelar", idReserva).header("Authorization", bearer(token(email))));
    }

    private ResultActions noPresentado(String email, int idReserva) throws Exception {
        return mvc.perform(patch("/api/admin/reservas/{id}/no-presentado", idReserva)
                .header("Authorization", bearer(token(email))));
    }

    private String estadoFranja(int idInstalacion, String fecha, int indice) throws Exception {
        String cuerpo = mvc.perform(get("/api/instalaciones/{id}/disponibilidad", idInstalacion).param("fecha", fecha)
                        .header("Authorization", bearer(token(ALUMNA))))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.franjas[" + indice + "].estado");
    }

    @Nested
    @DisplayName("Crear")
    class Crear {

        @Test
        void reserva_una_franja_libre_y_la_franja_pasa_a_reservada() throws Exception {
            assertThat(estadoFranja(INST_TENIS_2, "2026-10-28", 2)).isEqualTo("LIBRE"); // 10:00

            reservar(ALUMNA, INST_TENIS_2, "2026-10-28", "10:00")
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/reservas/")))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.instalacion").value("Pista de tenis 2"))
                    .andExpect(jsonPath("$.horaInicio").value("10:00:00"))
                    .andExpect(jsonPath("$.horaFin").value("11:00:00"))
                    .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                    .andExpect(jsonPath("$.cancelable").value(true));

            assertThat(estadoFranja(INST_TENIS_2, "2026-10-28", 2)).isEqualTo("RESERVADA");
        }

        @Test
        void la_hora_de_fin_la_decide_la_duracion_de_franja_de_la_instalacion() throws Exception {
            reservar(ALUMNA, INST_PADEL_2, "2026-10-30", "08:00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.horaFin").value("09:30:00"));
        }

        @Test
        void una_franja_ya_reservada_da_409() throws Exception {
            reservar(PAS, INST_PADEL_1, "2026-10-27", "09:30")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_OCUPADA"));
        }

        @Test
        void una_franja_bloqueada_da_409() throws Exception {
            reservar(ALUMNA, INST_PABELLON, "2026-10-26", "10:00")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_BLOQUEADA"))
                    .andExpect(jsonPath("$.detail", containsString("DOCENCIA")));
        }

        @Test
        void una_hora_que_no_es_inicio_de_franja_da_422() throws Exception {
            reservar(ALUMNA, INST_PADEL_2, "2026-10-30", "09:00") // pádel: 08:00, 09:30, 11:00...
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_NO_VALIDA"));
        }

        @Test
        void fuera_del_horario_de_apertura_da_422() throws Exception {
            reservar(ALUMNA, INST_TENIS_1, "2026-11-01", "10:00") // domingo
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_NO_VALIDA"));
            reservar(ALUMNA, INST_TENIS_1, "2026-10-30", "22:00") // cierra a las 22:00
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void en_el_pasado_da_422() throws Exception {
            reservar(ALUMNA, INST_TENIS_1, "2026-10-23", "09:00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RESERVA_EN_PASADO"));
        }

        @Test
        void instalacion_inexistente_da_404() throws Exception {
            reservar(ALUMNA, 999, "2026-10-30", "10:00").andExpect(status().isNotFound());
        }

        @Test
        void peticion_incompleta_da_400() throws Exception {
            mvc.perform(post("/api/reservas").header("Authorization", bearer(token(ALUMNA)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"idInstalacion\":3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.fecha").exists())
                    .andExpect(jsonPath("$.errores.horaInicio").exists());
        }

        @Test
        void json_mal_formado_da_400_con_codigo() throws Exception {
            mvc.perform(post("/api/reservas").header("Authorization", bearer(token(ALUMNA)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"idInstalacion\":3,"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"))
                    .andExpect(jsonPath("$.detail").value("La petición no tiene un formato válido"));
            mvc.perform(post("/api/reservas").header("Authorization", bearer(token(ALUMNA)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idInstalacion\":3,\"fecha\":\"27/10/2026\",\"horaInicio\":\"10:00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
        }

        @Test
        void sin_sesion_no_se_puede_reservar() throws Exception {
            mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idInstalacion\":3,\"fecha\":\"2026-10-30\",\"horaInicio\":\"10:00\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Reglas acordadas P1–P4 y P7")
    class Reglas {

        @Test
        void P1_solo_una_reserva_por_dia_y_cancelar_libera_el_cupo() throws Exception {
            reservar(ALUMNA, INST_TENIS_2, "2026-10-27", "10:00") // ya tiene pádel el 27/10
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("LIMITE_DIARIO"));

            cancelar(ALUMNA, RESERVA_ALUMNA_PADEL).andExpect(status().isOk());

            reservar(ALUMNA, INST_TENIS_2, "2026-10-27", "10:00").andExpect(status().isCreated());
        }

        @Test
        void P1_otro_dia_si_se_puede_reservar() throws Exception {
            reservar(ALUMNA, INST_TENIS_2, "2026-10-28", "10:00").andExpect(status().isCreated());
        }

        @Test
        void P2_hacen_falta_tres_horas_de_antelacion() throws Exception {
            reservar(ALUMNA, INST_TENIS_1, "2026-10-23", "12:00") // son las 10:00
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ANTELACION_MINIMA"));
            reservar(ALUMNA, INST_TENIS_1, "2026-10-23", "13:00").andExpect(status().isCreated());
        }

        @Test
        void P2_no_hay_antelacion_maxima() throws Exception {
            reservar(ALUMNA, INST_TENIS_1, "2027-03-02", "10:00").andExpect(status().isCreated());
        }

        @Test
        void P3_no_presentarse_bloquea_las_reservas_el_resto_de_la_semana() throws Exception {
            // Reserva de hoy a las 08:00, ya pasada, a la que la alumna no se presentó
            jdbc.update("INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin) "
                    + "VALUES (2, ?, DATE '2026-10-23', TIME '08:00', TIME '09:00')", INST_TENIS_2);
            Integer id = jdbc.queryForObject("SELECT max(id_reserva) FROM reserva", Integer.class);

            noPresentado(ADMIN, id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("NO_PRESENTADO"));

            Map<String, Object> sancion = jdbc.queryForMap(
                    "SELECT fecha_inicio, fecha_fin FROM sancion WHERE id_reserva = ?", id);
            assertThat(sancion.get("fecha_inicio").toString()).isEqualTo("2026-10-23");
            assertThat(sancion.get("fecha_fin").toString()).isEqualTo("2026-10-25"); // domingo

            reservar(ALUMNA, INST_TENIS_1, "2026-10-30", "10:00")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("USUARIO_SANCIONADO"))
                    .andExpect(jsonPath("$.detail", containsString("26/10/2026")));
            // El resto de usuarios no se ve afectado
            reservar(PAS, INST_TENIS_1, "2026-10-30", "10:00").andExpect(status().isCreated());
        }

        @Test
        void P3_solo_el_administrador_marca_no_presentado_y_solo_si_ya_empezo() throws Exception {
            noPresentado(ALUMNA, RESERVA_ALUMNA_PADEL).andExpect(status().isForbidden());
            noPresentado(ADMIN, RESERVA_ALUMNA_PADEL) // es el 27/10: aún no ha empezado
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RESERVA_NO_INICIADA"));
        }

        @Test
        void P4_los_usuarios_externos_no_hacen_reservas_individuales() throws Exception {
            reservar(EXTERNO, INST_TENIS_1, "2026-10-30", "12:00")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_NO_AUTORIZADO"));
        }

        @Test
        void P4_la_comunidad_universitaria_si() throws Exception {
            reservar(PAS, INST_TENIS_1, "2026-10-30", "12:00").andExpect(status().isCreated());
            reservar(PDI, INST_TENIS_1, "2026-10-30", "13:00").andExpect(status().isCreated());
        }

        @Test
        void P7_un_bloqueo_del_administrador_cancela_las_reservas_que_pisa() throws Exception {
            jdbc.update("INSERT INTO bloqueo (id_instalacion, id_creador, inicio, fin, motivo) "
                    + "VALUES (?, 1, TIMESTAMP '2026-10-27 09:00', TIMESTAMP '2026-10-27 12:00', 'MANTENIMIENTO')",
                    INST_PADEL_1);

            mvc.perform(get("/api/reservas/mias").header("Authorization", bearer(token(ALUMNA))))
                    .andExpect(jsonPath("$[0].id").value(RESERVA_ALUMNA_PADEL))
                    .andExpect(jsonPath("$[0].estado").value("CANCELADA"));
            assertThat(estadoFranja(INST_PADEL_1, "2026-10-27", 1)).isEqualTo("BLOQUEADA");
        }
    }

    @Nested
    @DisplayName("Mis reservas y cancelar")
    class MisReservasYCancelar {

        @Test
        void cada_usuario_solo_ve_sus_reservas() throws Exception {
            mvc.perform(get("/api/reservas/mias").header("Authorization", bearer(token(ALUMNA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RESERVA_ALUMNA_PADEL))
                    .andExpect(jsonPath("$[0].instalacion").value("Pista de pádel 1"))
                    .andExpect(jsonPath("$[0].cancelable").value(true));

            reservar(ALUMNA, INST_TENIS_2, "2026-10-28", "17:00").andExpect(status().isCreated());

            mvc.perform(get("/api/reservas/mias").header("Authorization", bearer(token(ALUMNA))))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].fecha").value("2026-10-28")); // más reciente primero
        }

        @Test
        void cancelar_libera_la_franja_al_instante() throws Exception {
            assertThat(estadoFranja(INST_PADEL_1, "2026-10-27", 1)).isEqualTo("RESERVADA");

            cancelar(ALUMNA, RESERVA_ALUMNA_PADEL)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"))
                    .andExpect(jsonPath("$.cancelable").value(false));

            assertThat(estadoFranja(INST_PADEL_1, "2026-10-27", 1)).isEqualTo("LIBRE");
            reservar(PAS, INST_PADEL_1, "2026-10-27", "09:30").andExpect(status().isCreated());
        }

        @Test
        void no_se_puede_cancelar_una_reserva_ajena() throws Exception {
            cancelar(PDI, RESERVA_ALUMNA_PADEL)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("RESERVA_AJENA"));
        }

        @Test
        void el_administrador_puede_cancelar_cualquier_reserva() throws Exception {
            cancelar(ADMIN, RESERVA_ALUMNA_PADEL).andExpect(status().isOk());
        }

        @Test
        void no_se_cancela_dos_veces() throws Exception {
            cancelar(ALUMNA, RESERVA_ALUMNA_PADEL).andExpect(status().isOk());
            cancelar(ALUMNA, RESERVA_ALUMNA_PADEL)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("RESERVA_NO_CANCELABLE"));
        }

        @Test
        void cancelar_una_reserva_inexistente_da_404() throws Exception {
            cancelar(ALUMNA, 999).andExpect(status().isNotFound());
        }
    }
}
