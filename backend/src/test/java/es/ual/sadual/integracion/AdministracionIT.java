package es.ual.sadual.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Panel de administración del núcleo común (F06, F07, F08, F10) e informes (P9).
 * Reloj de pruebas: viernes 23/10/2026 10:00. Seed: reservas del 27/10 (alumna pádel 1 09:30,
 * PDI tenis 1 18:00), 28/10 (PAS piscina 1 14:00) e institucional pendiente el 29/10 (fútbol 7).
 */
@DisplayName("Panel de administración (F06, F07, F08, F10) e informes (P9)")
class AdministracionIT extends AbstractIntegracionIT {

    private static final String ADMIN = "admin.deportes@ual.es";
    private static final String ALUMNA = "alumna.demo@ual.es";
    private static final String REPRESENTANTE = "externo.demo@correo.test";
    private static final int TIPO_TENIS = 2, ID_ADMIN = 1, ID_REPRESENTANTE = 5;

    @Autowired
    private ObjectMapper json;

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String email, Object cuerpo) throws Exception {
        peticion.header("Authorization", bearer(token(email)));
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        }
        return mvc.perform(peticion);
    }

    private static int id(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    private String franjas(int instalacion, String fecha) throws Exception {
        return enviar(get("/api/instalaciones/{id}/disponibilidad", instalacion).param("fecha", fecha), ALUMNA, null)
                .andReturn().getResponse().getContentAsString();
    }

    // =====================================================================================
    @Nested
    @DisplayName("F06 · Instalaciones")
    class Instalaciones {

        private Map<String, Object> instalacion(String nombre) {
            return new HashMap<>(Map.of("idTipo", TIPO_TENIS, "nombre", nombre, "aforo", 4, "duracionFranjaMin", 60));
        }

        private int crear(String nombre) throws Exception {
            return id(enviar(post("/api/admin/instalaciones"), ADMIN, instalacion(nombre))
                    .andExpect(status().isCreated()), "$.id");
        }

        @Test
        void sin_horario_se_aplica_el_horario_general_P11_y_ya_se_puede_consultar() throws Exception {
            int id = id(enviar(post("/api/admin/instalaciones"), ADMIN, instalacion("Pista de tenis 3"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.activa").value(true))
                    .andExpect(jsonPath("$.horario", hasSize(6)))
                    .andExpect(jsonPath("$.reservasFuturas").value(0)), "$.id");

            assertThat((List<?>) JsonPath.read(franjas(id, "2026-10-26"), "$.franjas")).hasSize(14);  // lunes 8-22
            assertThat((List<?>) JsonPath.read(franjas(id, "2026-10-31"), "$.franjas")).hasSize(5);   // sábado 9-14
        }

        @Test
        void P12_la_duracion_de_franja_y_el_horario_se_editan_desde_el_panel() throws Exception {
            int id = crear("Pista de tenis 4");
            Map<String, Object> cambios = instalacion("Pista de tenis 4");
            cambios.put("duracionFranjaMin", 90);
            cambios.put("horario", List.of(Map.of("diaSemana", 1, "horaApertura", "10:00", "horaCierre", "13:00")));
            enviar(put("/api/admin/instalaciones/{id}", id), ADMIN, cambios)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.duracionFranjaMin").value(90))
                    .andExpect(jsonPath("$.horario", hasSize(1)));

            assertThat((List<String>) JsonPath.read(franjas(id, "2026-10-26"), "$.franjas[*].horaInicio"))
                    .containsExactly("10:00:00", "11:30:00");
            assertThat((List<?>) JsonPath.read(franjas(id, "2026-10-27"), "$.franjas")).isEmpty();
        }

        @Test
        void se_rechazan_nombres_repetidos_y_horarios_solapados() throws Exception {
            enviar(post("/api/admin/instalaciones"), ADMIN, instalacion("Pista de pádel 1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("INSTALACION_DUPLICADA"));

            Map<String, Object> mala = instalacion("Pista mala");
            mala.put("horario", List.of(Map.of("diaSemana", 1, "horaApertura", "08:00", "horaCierre", "12:00"),
                    Map.of("diaSemana", 1, "horaApertura", "11:00", "horaCierre", "14:00")));
            enviar(post("/api/admin/instalaciones"), ADMIN, mala)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("HORARIO_NO_VALIDO"));
        }

        @Test
        void no_se_da_de_baja_con_reservas_futuras_y_al_darla_de_baja_desaparece_del_catalogo() throws Exception {
            int id = crear("Pista de tenis 5");
            int reserva = id(enviar(post("/api/reservas"), ALUMNA,
                    Map.of("idInstalacion", id, "fecha", "2026-10-28", "horaInicio", "10:00"))
                    .andExpect(status().isCreated()), "$.id");

            enviar(patch("/api/admin/instalaciones/{id}/desactivar", id), ADMIN, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("INSTALACION_CON_RESERVAS"));

            enviar(patch("/api/reservas/{id}/cancelar", reserva), ALUMNA, null).andExpect(status().isOk());
            enviar(patch("/api/admin/instalaciones/{id}/desactivar", id), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activa").value(false));
            enviar(get("/api/instalaciones"), ALUMNA, null)
                    .andExpect(jsonPath("$[*].nombre", not(hasItem("Pista de tenis 5"))));

            enviar(patch("/api/admin/instalaciones/{id}/activar", id), ADMIN, null)
                    .andExpect(jsonPath("$.activa").value(true));
        }

        @Test
        void solo_la_administracion_gestiona_instalaciones() throws Exception {
            enviar(get("/api/admin/instalaciones"), ALUMNA, null).andExpect(status().isForbidden());
            enviar(get("/api/admin/instalaciones"), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(9)));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("F07 · Usuarios")
    class Usuarios {

        private Map<String, Object> alta(String email, String rol) {
            return Map.of("nombre", "Nuevo", "apellidos", "Miembro UAL", "email", email, "rol", rol,
                    "password", "Inicial2026!");
        }

        @Test
        void P16_la_administracion_da_de_alta_a_la_comunidad_universitaria_y_puede_entrar() throws Exception {
            enviar(post("/api/admin/usuarios"), ADMIN, alta("Nuevo.Pdi@ual.es", "PDI"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("nuevo.pdi@ual.es"))
                    .andExpect(jsonPath("$.rol").value("PDI"))
                    .andExpect(jsonPath("$.activo").value(true));

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nuevo.pdi@ual.es\",\"password\":\"Inicial2026!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.puedeReservar").value(true));

            enviar(get("/api/admin/usuarios").param("texto", "miembro ual"), ADMIN, null)
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void los_externos_no_se_dan_de_alta_desde_el_panel_y_no_se_repiten_emails() throws Exception {
            enviar(post("/api/admin/usuarios"), ADMIN, alta("ext@correo.test", "EXTERNO"))
                    .andExpect(status().isBadRequest());
            enviar(post("/api/admin/usuarios"), ADMIN, alta(ALUMNA, "ALUMNADO"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("EMAIL_EN_USO"));
        }

        @Test
        void cambiar_el_perfil_y_desactivar_impide_entrar() throws Exception {
            int id = id(enviar(post("/api/admin/usuarios"), ADMIN, alta("cambio@ual.es", "ALUMNADO"))
                    .andExpect(status().isCreated()), "$.id");
            enviar(patch("/api/admin/usuarios/{id}/rol", id), ADMIN, Map.of("rol", "PAS"))
                    .andExpect(jsonPath("$.rol").value("PAS"));
            enviar(patch("/api/admin/usuarios/{id}/desactivar", id), ADMIN, null)
                    .andExpect(jsonPath("$.activo").value(false));

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"cambio@ual.es\",\"password\":\"Inicial2026!\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
        }

        @Test
        void un_representante_solo_puede_ser_externo_y_el_admin_no_se_desactiva_a_si_mismo() throws Exception {
            enviar(patch("/api/admin/usuarios/{id}/rol", ID_REPRESENTANTE), ADMIN, Map.of("rol", "ALUMNADO"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("REPRESENTANTE_NO_VALIDO"));
            enviar(patch("/api/admin/usuarios/{id}/desactivar", ID_ADMIN), ADMIN, null)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("OPERACION_SOBRE_SI_MISMO"));
            enviar(get("/api/admin/usuarios").param("rol", "EXTERNO"), ADMIN, null)
                    .andExpect(jsonPath("$[*].entidad", hasItem("Club Deportivo Demo Almería")));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("F08 · Bloqueos y cierres")
    class Bloqueos {

        private Map<String, Object> bloqueo(int instalacion, String inicio, String fin, String motivo) {
            return Map.of("idInstalacion", instalacion, "inicio", inicio, "fin", fin, "motivo", motivo,
                    "observaciones", "Prueba");
        }

        @Test
        void P7_un_bloqueo_cancela_las_reservas_que_pisa_y_se_ve_en_la_disponibilidad() throws Exception {
            enviar(post("/api/admin/bloqueos"), ADMIN,
                    bloqueo(INST_TENIS_1, "2026-10-27T17:00:00", "2026-10-27T20:00:00", "MANTENIMIENTO"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reservasCanceladas").value(1))
                    .andExpect(jsonPath("$.bloqueo.motivo").value("MANTENIMIENTO"));

            assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id_reserva = 2", String.class))
                    .isEqualTo("CANCELADA");
            List<String> estados = JsonPath.read(franjas(INST_TENIS_1, "2026-10-27"),
                    "$.franjas[?(@.horaInicio == '18:00:00')].motivo");
            assertThat(estados).containsExactly("MANTENIMIENTO");
        }

        @Test
        void no_se_solapan_bloqueos_ni_se_crean_eventos_desde_aqui() throws Exception {
            enviar(post("/api/admin/bloqueos"), ADMIN,
                    bloqueo(INST_PABELLON, "2026-10-26T12:00:00", "2026-10-26T15:00:00", "OTRO"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("INSTALACION_NO_DISPONIBLE"))
                    .andExpect(jsonPath("$.detail", containsString("DOCENCIA")));
            enviar(post("/api/admin/bloqueos"), ADMIN,
                    bloqueo(INST_SALA, "2026-10-26T12:00:00", "2026-10-26T15:00:00", "EVENTO"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void al_borrar_un_bloqueo_la_franja_queda_libre_pero_los_de_eventos_no_se_borran_aqui() throws Exception {
            int id = id(enviar(post("/api/admin/bloqueos"), ADMIN,
                    bloqueo(INST_SALA, "2026-10-28T10:00:00", "2026-10-28T12:00:00", "DOCENCIA"))
                    .andExpect(status().isCreated()), "$.bloqueo.id");
            enviar(delete("/api/admin/bloqueos/{id}", id), ADMIN, null).andExpect(status().isNoContent());
            List<String> estado = JsonPath.read(franjas(INST_SALA, "2026-10-28"),
                    "$.franjas[?(@.horaInicio == '10:00:00')].estado");
            assertThat(estado).containsExactly("LIBRE");

            Integer delEvento = jdbc.queryForObject("SELECT min(id_bloqueo) FROM bloqueo WHERE id_evento IS NOT NULL",
                    Integer.class);
            enviar(delete("/api/admin/bloqueos/{id}", delEvento), ADMIN, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("BLOQUEO_GESTIONADO"));
        }

        @Test
        void P11_un_cierre_general_cierra_todas_las_instalaciones() throws Exception {
            enviar(post("/api/admin/cierres"), ADMIN, Map.of("inicio", "2026-10-30T00:00:00",
                    "fin", "2026-10-31T00:00:00", "observaciones", "Cierre por obras en la red eléctrica"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bloqueosCreados").value(9));
            enviar(get("/api/instalaciones/{id}/disponibilidad", INST_PADEL_2).param("fecha", "2026-10-30"), ALUMNA, null)
                    .andExpect(jsonPath("$.franjas[*].motivo", everyItem(is("CIERRE"))));
            enviar(get("/api/admin/bloqueos").param("desde", "2026-10-30").param("hasta", "2026-10-30"), ADMIN, null)
                    .andExpect(jsonPath("$", hasSize(9)));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("F10 · Consulta general de reservas")
    class Consulta {

        @Test
        void filtra_por_fechas_estado_y_texto() throws Exception {
            enviar(get("/api/admin/reservas").param("desde", "2026-10-27").param("hasta", "2026-10-29"), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)));
            enviar(get("/api/admin/reservas").param("desde", "2026-10-27").param("estado", "PENDIENTE"), ADMIN, null)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].entidad").value("Club Deportivo Demo Almería"));
            enviar(get("/api/admin/reservas").param("desde", "2026-10-27").param("texto", "lucía"), ADMIN, null)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].instalacion").value("Pista de pádel 1"));
            enviar(get("/api/admin/reservas"), ALUMNA, null).andExpect(status().isForbidden());
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("P9 · Informes")
    class Informes {

        @Test
        void informe_semanal_de_ocupacion_y_uso_por_perfil() throws Exception {
            String cuerpo = enviar(get("/api/admin/informes").param("periodo", "SEMANAL").param("fecha", "2026-10-28"),
                    ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value("2026-10-26"))
                    .andExpect(jsonPath("$.hasta").value("2026-11-01"))
                    .andExpect(jsonPath("$.ocupacionPorInstalacion", hasSize(9)))
                    .andReturn().getResponse().getContentAsString();

            List<Double> padel = JsonPath.read(cuerpo, "$.ocupacionPorInstalacion[?(@.instalacion == 'Pista de pádel 1')].porcentaje");
            List<Double> apertura = JsonPath.read(cuerpo, "$.ocupacionPorInstalacion[?(@.instalacion == 'Pista de pádel 1')].horasApertura");
            List<Double> bloqueadas = JsonPath.read(cuerpo, "$.ocupacionPorInstalacion[?(@.instalacion == 'Pabellón cubierto')].horasBloqueadas");
            assertThat(apertura).containsExactly(75.0);       // L-V 14 h + sábado 5 h
            assertThat(padel).containsExactly(2.0);           // 1,5 h de 75 h
            assertThat(bloqueadas).containsExactly(6.0);      // docencia del lunes 8-14
            List<String> perfiles = JsonPath.read(cuerpo, "$.usoPorPerfil[*].rol");
            assertThat(perfiles).containsExactlyInAnyOrder("ALUMNADO", "PDI", "PAS");  // la institucional está pendiente
        }

        @Test
        void cuenta_cancelaciones_y_no_presentados_del_periodo() throws Exception {
            jdbc.update("""
                    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin, estado)
                    VALUES (2, 3, DATE '2026-10-21', TIME '10:00', TIME '11:00', 'CONFIRMADA'),
                           (3, 4, DATE '2026-10-22', TIME '10:00', TIME '11:00', 'CONFIRMADA')
                    """);
            jdbc.update("UPDATE reserva SET estado = 'NO_PRESENTADO' WHERE fecha = DATE '2026-10-21'");
            jdbc.update("UPDATE reserva SET estado = 'CANCELADA', fecha_cancelacion = now() WHERE fecha = DATE '2026-10-22'");

            enviar(get("/api/admin/informes").param("periodo", "MENSUAL").param("fecha", "2026-10-05"), ADMIN, null)
                    .andExpect(jsonPath("$.desde").value("2026-10-01"))
                    .andExpect(jsonPath("$.hasta").value("2026-10-31"))
                    .andExpect(jsonPath("$.noPresentados.total").value(1))
                    .andExpect(jsonPath("$.noPresentados.porUsuario[0].usuario").value("Lucía Martín Ortega"))
                    .andExpect(jsonPath("$.cancelaciones.canceladas").value(1))
                    .andExpect(jsonPath("$.cancelaciones.porInstalacion[0].instalacion").value("Pista de tenis 2"));
        }

        @Test
        void solo_hay_informes_semanales_y_mensuales() throws Exception {
            enviar(get("/api/admin/informes").param("periodo", "ANUAL"), ADMIN, null)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PERIODO_NO_VALIDO"));
        }
    }
}
