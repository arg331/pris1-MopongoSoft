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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Variación 7 · Incremento 2 (P17–P22) contra PostgreSQL real.
 * Reloj de pruebas: viernes 23/10/2026 10:00. Seed: el representante externo.demo (Club Deportivo
 * Demo Almería) tiene la reserva institucional 4 PENDIENTE (fútbol 7, 29/10 19-21); la tutora
 * tutor.demo tiene a Hugo (2017) en la escuela de natación (calle 2 de la piscina, L y X 17-18);
 * la alumna está inscrita en el evento del 19/12 (pabellón y sala A).
 */
@DisplayName("Variación 7 · Registro, reservas institucionales, eventos y escuelas")
class Variacion7IT extends AbstractIntegracionIT {

    private static final String ALUMNA = "alumna.demo@ual.es";
    private static final String PDI = "pdi.demo@ual.es";
    private static final String REPRESENTANTE = "externo.demo@correo.test";
    private static final String TUTORA = "tutor.demo@correo.test";
    private static final String ADMIN = "admin.deportes@ual.es";

    @Autowired
    private ObjectMapper json;

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String email, Object cuerpo) throws Exception {
        if (email != null) {
            peticion.header("Authorization", bearer(token(email)));
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        }
        return mvc.perform(peticion);
    }

    private String estadoFranja(int idInstalacion, String fecha, String hora) throws Exception {
        String cuerpo = enviar(get("/api/instalaciones/{id}/disponibilidad", idInstalacion).param("fecha", fecha),
                ALUMNA, null).andReturn().getResponse().getContentAsString();
        List<String> estados = JsonPath.read(cuerpo, "$.franjas[?(@.horaInicio == '" + hora + ":00')].estado");
        return estados.getFirst();
    }

    private String estadoReserva(int idReserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id_reserva = ?", String.class, idReserva);
    }

    private static int id(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    // =====================================================================================
    @Nested
    @DisplayName("P17 · Registro de tutores y representantes")
    class Registro {

        private Map<String, Object> registro(String tipo, String email, String cif) {
            Map<String, Object> m = new java.util.HashMap<>(Map.of("tipo", tipo, "nombre", "Nuevo",
                    "apellidos", "Usuario Prueba", "email", email, "password", "Segura2026!", "telefono", "600111222"));
            if (cif != null) {
                m.put("cifEntidad", cif);
            }
            return m;
        }

        @Test
        void un_tutor_se_registra_como_externo_sin_reservas_individuales() throws Exception {
            ResultActions r = enviar(post("/api/auth/registro"), null, registro("TUTOR", "Tutor.Nuevo@correo.test", null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.usuario.email").value("tutor.nuevo@correo.test"))
                    .andExpect(jsonPath("$.usuario.rol").value("EXTERNO"))
                    .andExpect(jsonPath("$.usuario.puedeReservar").value(false));

            String token = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.token");
            mvc.perform(post("/api/reservas").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idInstalacion\":3,\"fecha\":\"2026-10-28\",\"horaInicio\":\"10:00\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_NO_AUTORIZADO"));
        }

        @Test
        void P24_el_telefono_es_obligatorio_para_los_tutores_y_opcional_para_los_representantes() throws Exception {
            Map<String, Object> sinTelefono = registro("TUTOR", "sin.telefono@correo.test", null);
            sinTelefono.remove("telefono");
            enviar(post("/api/auth/registro"), null, sinTelefono)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("TELEFONO_OBLIGATORIO"));

            Map<String, Object> representante = registro("REPRESENTANTE", "rep.sin.telefono@club.test", "G00000000");
            representante.remove("telefono");
            enviar(post("/api/auth/registro"), null, representante).andExpect(status().isCreated());
        }

        @Test
        void P23_el_representante_queda_pendiente_hasta_que_la_administracion_lo_activa() throws Exception {
            enviar(post("/api/auth/registro"), null, registro("REPRESENTANTE", "rep.nuevo@club.test", "g00000000"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pendienteValidacion").value(true))
                    .andExpect(jsonPath("$.token").doesNotExist());
            Integer id = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE email = 'rep.nuevo@club.test'",
                    Integer.class);
            assertThat(jdbc.queryForObject("SELECT id_entidad FROM usuario WHERE id_usuario = ?", Integer.class, id))
                    .isEqualTo(1);

            String login = "{\"email\":\"rep.nuevo@club.test\",\"password\":\"Segura2026!\"}";
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"))
                    .andExpect(jsonPath("$.detail", containsString("pendiente")));

            enviar(get("/api/admin/entidades"), ADMIN, null)
                    .andExpect(jsonPath("$[0].representantesPendientes").value(1));
            enviar(patch("/api/admin/usuarios/{id}/activar", id), ADMIN, null).andExpect(status().isOk());

            String token = JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(login)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
            mvc.perform(post("/api/reservas-institucionales").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idInstalacion\":5,\"fecha\":\"2026-10-30\",\"horaInicio\":\"10:00\",\"horaFin\":\"11:00\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        }

        @Test
        void un_representante_necesita_una_entidad_con_convenio_vigente() throws Exception {
            enviar(post("/api/auth/registro"), null, registro("REPRESENTANTE", "rep.x@club.test", "B12345678"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ENTIDAD_NO_VALIDA"));
            enviar(post("/api/auth/registro"), null, registro("REPRESENTANTE", "rep.y@club.test", null))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ENTIDAD_OBLIGATORIA"));
        }

        @Test
        void no_se_repite_un_email_ni_se_aceptan_contrasenas_cortas() throws Exception {
            enviar(post("/api/auth/registro"), null, registro("TUTOR", "ALUMNA.demo@ual.es", null))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("EMAIL_EN_USO"));

            Map<String, Object> corta = registro("TUTOR", "corta@correo.test", null);
            corta.put("password", "123");
            enviar(post("/api/auth/registro"), null, corta)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                    .andExpect(jsonPath("$.errores.password").exists());
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("P21 · Reservas institucionales y autorización")
    class Institucionales {

        private ResultActions solicitar(String email, int instalacion, String fecha, String inicio, String fin)
                throws Exception {
            return enviar(post("/api/reservas-institucionales"), email,
                    Map.of("idInstalacion", instalacion, "fecha", fecha, "horaInicio", inicio, "horaFin", fin));
        }

        @Test
        void el_representante_reserva_varias_franjas_y_quedan_pendientes_ocupando_la_franja() throws Exception {
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "18:00", "20:00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                    .andExpect(jsonPath("$.institucional").value(true))
                    .andExpect(jsonPath("$.horaFin").value("20:00:00"));

            assertThat(estadoFranja(INST_FUTBOL, "2026-10-30", "18:00")).isEqualTo("RESERVADA");
            assertThat(estadoFranja(INST_FUTBOL, "2026-10-30", "19:00")).isEqualTo("RESERVADA");
        }

        @Test
        void no_cuentan_para_el_limite_de_una_reserva_al_dia() throws Exception {
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "10:00", "11:00").andExpect(status().isCreated());
            solicitar(REPRESENTANTE, INST_TENIS_2, "2026-10-30", "10:00", "12:00").andExpect(status().isCreated());
        }

        @Test
        void solo_las_hace_un_representante() throws Exception {
            solicitar(ALUMNA, INST_FUTBOL, "2026-10-30", "10:00", "11:00")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("NO_REPRESENTANTE"));
            solicitar(TUTORA, INST_FUTBOL, "2026-10-30", "10:00", "11:00")
                    .andExpect(status().isForbidden());
        }

        @Test
        void el_periodo_debe_encajar_en_franjas_libres() throws Exception {
            // pádel: franjas de 90 min desde las 08:00
            solicitar(REPRESENTANTE, INST_PADEL_2, "2026-10-30", "08:00", "10:00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_NO_VALIDA"));
            solicitar(REPRESENTANTE, INST_PADEL_2, "2026-10-30", "08:00", "11:00").andExpect(status().isCreated());
            // tenis 1: el PDI tiene las 18:00 del 27/10
            solicitar(REPRESENTANTE, INST_TENIS_1, "2026-10-27", "17:00", "19:00")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_OCUPADA"));
            // pabellón bloqueado por docencia el 26/10 por la mañana
            solicitar(REPRESENTANTE, INST_PABELLON, "2026-10-26", "09:00", "10:00")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("FRANJA_BLOQUEADA"));
        }

        @Test
        void P27_se_solicitan_con_48_horas_de_antelacion() throws Exception {
            // ahora: viernes 23/10 10:00 → sábado 24/10 10:00 (24 h, no vale) y lunes 26/10 09:00 (71 h, vale)
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-24", "10:00", "11:00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ANTELACION_MINIMA"))
                    .andExpect(jsonPath("$.detail", containsString("48 horas")));
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-26", "09:00", "10:00").andExpect(status().isCreated());
        }

        @Test
        void P28_como_maximo_4_horas_seguidas_por_reserva() throws Exception {
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "10:00", "15:00")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("DURACION_MAXIMA"));
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "10:00", "14:00").andExpect(status().isCreated());
        }

        @Test
        void P32_con_tres_ausencias_la_administracion_puede_suspender_a_la_entidad() throws Exception {
            enviar(patch("/api/admin/entidades/{id}/suspender", 1), ADMIN, Map.of("hasta", "2026-11-30", "motivo", "x"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("AUSENCIAS_INSUFICIENTES"));

            jdbc.update("""
                    INSERT INTO reserva (id_usuario, id_instalacion, id_entidad, id_autorizador, fecha, hora_inicio,
                                         hora_fin, estado, fecha_resolucion)
                    SELECT 5, 5, 1, 1, d, TIME '10:00', TIME '11:00', 'NO_PRESENTADO', TIMESTAMP '2026-09-30 10:00'
                      FROM (VALUES (DATE '2026-10-05'), (DATE '2026-10-06'), (DATE '2026-10-07')) AS f(d)
                    """);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM sancion", Integer.class)).isZero();
            enviar(get("/api/admin/entidades"), ADMIN, null)
                    .andExpect(jsonPath("$[0].ausencias").value(3))
                    .andExpect(jsonPath("$[0].puedeSuspenderse").value(true));

            enviar(patch("/api/admin/entidades/{id}/suspender", 1), ADMIN,
                    Map.of("hasta", "2026-11-30", "motivo", "Tres ausencias sin justificar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suspendidaHasta").value("2026-11-30"));
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "10:00", "11:00")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ENTIDAD_SUSPENDIDA"));

            enviar(patch("/api/admin/entidades/{id}/levantar-suspension", 1), ADMIN, null).andExpect(status().isOk());
            solicitar(REPRESENTANTE, INST_FUTBOL, "2026-10-30", "10:00", "11:00").andExpect(status().isCreated());
        }

        @Test
        void la_administracion_ve_las_pendientes_y_las_autoriza() throws Exception {
            enviar(get("/api/admin/reservas-institucionales").param("estado", "PENDIENTE"), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RESERVA_INSTITUCIONAL_PENDIENTE))
                    .andExpect(jsonPath("$[0].entidad").value("Club Deportivo Demo Almería"))
                    .andExpect(jsonPath("$[0].emailRepresentante").value(REPRESENTANTE));

            enviar(patch("/api/admin/reservas-institucionales/{id}/autorizar", RESERVA_INSTITUCIONAL_PENDIENTE), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
            assertThat(jdbc.queryForObject("SELECT id_autorizador FROM reserva WHERE id_reserva = 4", Integer.class))
                    .isEqualTo(1);

            enviar(patch("/api/admin/reservas-institucionales/{id}/autorizar", RESERVA_INSTITUCIONAL_PENDIENTE), ADMIN, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ESTADO_NO_VALIDO"));
        }

        @Test
        void un_rechazo_exige_motivo_y_libera_la_franja() throws Exception {
            enviar(patch("/api/admin/reservas-institucionales/{id}/rechazar", RESERVA_INSTITUCIONAL_PENDIENTE), ADMIN,
                    Map.of("motivo", " "))
                    .andExpect(status().isBadRequest());

            enviar(patch("/api/admin/reservas-institucionales/{id}/rechazar", RESERVA_INSTITUCIONAL_PENDIENTE), ADMIN,
                    Map.of("motivo", "Campo reservado para la liga interna"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                    .andExpect(jsonPath("$.motivoRechazo").value("Campo reservado para la liga interna"));

            assertThat(estadoFranja(INST_FUTBOL, "2026-10-29", "19:00")).isEqualTo("LIBRE");
            enviar(get("/api/reservas/mias"), REPRESENTANTE, null)
                    .andExpect(jsonPath("$[0].estado").value("RECHAZADA"))
                    .andExpect(jsonPath("$[0].cancelable").value(false));
        }

        @Test
        void solo_la_administracion_autoriza_y_el_representante_puede_cancelar_la_suya() throws Exception {
            enviar(patch("/api/admin/reservas-institucionales/{id}/autorizar", RESERVA_INSTITUCIONAL_PENDIENTE),
                    REPRESENTANTE, null)
                    .andExpect(status().isForbidden());

            enviar(patch("/api/reservas/{id}/cancelar", RESERVA_INSTITUCIONAL_PENDIENTE), REPRESENTANTE, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("P18 · Eventos masivos")
    class Eventos {

        private ResultActions crear(String nombre, String inicio, String fin, int aforo, List<Integer> instalaciones)
                throws Exception {
            return crear(nombre, inicio, fin, aforo, instalaciones, "COMUNIDAD_UAL");
        }

        private ResultActions crear(String nombre, String inicio, String fin, int aforo, List<Integer> instalaciones,
                                    String publico) throws Exception {
            return enviar(post("/api/admin/eventos"), ADMIN, Map.of("nombre", nombre, "inicio", inicio, "fin", fin,
                    "aforoMaximo", aforo, "idInstalaciones", instalaciones, "publico", publico));
        }

        private int crearPublicado(String nombre, int aforo) throws Exception {
            int id = id(crear(nombre, "2026-11-07T09:00:00", "2026-11-07T12:00:00", aforo, List.of(INST_SALA))
                    .andExpect(status().isCreated()), "$.evento.id");
            enviar(patch("/api/admin/eventos/{id}/publicar", id), ADMIN, null).andExpect(status().isOk());
            return id;
        }

        @Test
        void un_evento_ocupa_varias_instalaciones_y_cancela_las_reservas_que_pisa() throws Exception {
            int reserva = id(enviar(post("/api/reservas"), ALUMNA,
                    Map.of("idInstalacion", INST_TENIS_2, "fecha", "2026-10-31", "horaInicio", "10:00"))
                    .andExpect(status().isCreated()), "$.id");

            crear("Torneo de bienvenida", "2026-10-31T09:00:00", "2026-10-31T12:00:00", 80,
                    List.of(INST_TENIS_1, INST_TENIS_2))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.evento.estado").value("PLANIFICADO"))
                    .andExpect(jsonPath("$.evento.instalaciones", hasSize(2)))
                    .andExpect(jsonPath("$.reservasCanceladas").value(1));

            assertThat(estadoReserva(reserva)).isEqualTo("CANCELADA");
            assertThat(estadoFranja(INST_TENIS_1, "2026-10-31", "10:00")).isEqualTo("BLOQUEADA");
        }

        @Test
        void no_se_crea_sobre_una_instalacion_ya_bloqueada() throws Exception {
            crear("Evento solapado", "2026-10-26T10:00:00", "2026-10-26T12:00:00", 50, List.of(INST_SALA, INST_PABELLON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("INSTALACION_NO_DISPONIBLE"))
                    .andExpect(jsonPath("$.detail", containsString("DOCENCIA")));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM evento WHERE nombre = 'Evento solapado'", Integer.class))
                    .isZero();
        }

        @Test
        void solo_se_inscribe_en_eventos_publicados_y_una_sola_vez() throws Exception {
            int id = id(crear("Charla de nutrición", "2026-11-05T18:00:00", "2026-11-05T20:00:00", 30, List.of(INST_SALA))
                    .andExpect(status().isCreated()), "$.evento.id");
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("EVENTO_NO_DISPONIBLE"));

            enviar(patch("/api/admin/eventos/{id}/publicar", id), ADMIN, null)
                    .andExpect(jsonPath("$.estado").value("PUBLICADO"));
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inscrito").value(true))
                    .andExpect(jsonPath("$.plazasLibres").value(29));
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("YA_INSCRITO"));
        }

        @Test
        void el_aforo_lo_garantiza_la_base_de_datos() throws Exception {
            int id = crearPublicado("Clase magistral", 1);
            enviar(post("/api/eventos/{id}/inscripcion", id), ALUMNA, null).andExpect(status().isOk());
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("AFORO_COMPLETO"));

            // al anular, la plaza vuelve a quedar libre
            enviar(delete("/api/eventos/{id}/inscripcion", id), ALUMNA, null).andExpect(status().isOk());
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null).andExpect(status().isOk());
        }

        @Test
        void al_cancelar_el_evento_se_anulan_las_inscripciones_y_se_liberan_las_instalaciones() throws Exception {
            int id = crearPublicado("Exhibición cancelada", 20);
            enviar(post("/api/eventos/{id}/inscripcion", id), ALUMNA, null).andExpect(status().isOk());
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null).andExpect(status().isOk());
            assertThat(estadoFranja(INST_SALA, "2026-11-07", "10:00")).isEqualTo("BLOQUEADA");

            enviar(patch("/api/admin/eventos/{id}/cancelar", id), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evento.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.inscripcionesAnuladas").value(2));

            assertThat(estadoFranja(INST_SALA, "2026-11-07", "10:00")).isEqualTo("LIBRE");
            enviar(get("/api/admin/eventos/{id}/inscritos", id), ADMIN, null)
                    .andExpect(jsonPath("$[*].estado", hasItem("ANULADA")))
                    .andExpect(jsonPath("$[?(@.estado == 'ACTIVA')]", hasSize(0)));
            enviar(patch("/api/admin/eventos/{id}/cancelar", id), ADMIN, null).andExpect(status().isConflict());
        }

        @Test
        void los_usuarios_ven_los_eventos_publicados_y_si_estan_inscritos() throws Exception {
            enviar(get("/api/eventos"), ALUMNA, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(EVENTO_PUERTAS_ABIERTAS))
                    .andExpect(jsonPath("$[0].inscrito").value(true))
                    .andExpect(jsonPath("$[0].instalaciones", hasSize(2)));
            enviar(get("/api/eventos/mios"), PDI, null).andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void P33_la_administracion_decide_si_el_evento_admite_externos() throws Exception {
            int soloUal = crearPublicado("Torneo interno", 50);
            enviar(post("/api/eventos/{id}/inscripcion", soloUal), TUTORA, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_NO_AUTORIZADO"));
            enviar(get("/api/eventos"), TUTORA, null)
                    .andExpect(jsonPath("$[*].id", not(hasItem(soloUal))))
                    .andExpect(jsonPath("$[*].id", hasItem(EVENTO_PUERTAS_ABIERTAS)));
            // El evento del seed está abierto a la comunidad y a los externos
            enviar(post("/api/eventos/{id}/inscripcion", EVENTO_PUERTAS_ABIERTAS), TUTORA, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publico").value("COMUNIDAD_Y_EXTERNOS"));
        }

        @Test
        void P35_las_inscripciones_se_cierran_24_horas_antes() throws Exception {
            // ahora: viernes 23/10 10:00; el evento empieza el sábado 24/10 a las 09:00
            int id = id(crear("Exhibición de mañana", "2026-10-24T09:00:00", "2026-10-24T12:00:00", 30,
                    List.of(INST_SALA)).andExpect(status().isCreated()), "$.evento.id");
            enviar(patch("/api/admin/eventos/{id}/publicar", id), ADMIN, null)
                    .andExpect(jsonPath("$.cierreInscripcion").value("2026-10-23T09:00:00"));
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("INSCRIPCION_CERRADA"));
        }

        @Test
        void P58_una_sancion_por_no_presentarse_no_impide_inscribirse_en_eventos() throws Exception {
            jdbc.update("INSERT INTO sancion (id_usuario, motivo, fecha_inicio, fecha_fin) "
                    + "VALUES (3, 'Prueba', DATE '2026-10-19', DATE '2026-10-25')");
            int id = crearPublicado("Evento con sancionado", 10);
            enviar(post("/api/eventos/{id}/inscripcion", id), PDI, null).andExpect(status().isOk());
        }

        @Test
        void solo_la_administracion_organiza_y_no_se_inscribe() throws Exception {
            enviar(post("/api/admin/eventos"), ALUMNA, Map.of("nombre", "x")).andExpect(status().isForbidden());
            enviar(post("/api/eventos/{id}/inscripcion", EVENTO_PUERTAS_ABIERTAS), ADMIN, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_NO_AUTORIZADO"));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("P19, P20, P22 · Escuelas, menores e inscripciones")
    class Escuelas {

        private ResultActions crearEscuela(String nombre, int instalacion, String desde, String hasta, int edadMin,
                                           int edadMax, int plazas, List<Map<String, Object>> horario) throws Exception {
            return enviar(post("/api/admin/escuelas"), ADMIN, Map.of("idInstalacion", instalacion, "nombre", nombre,
                    "temporada", "2026-2027", "fechaInicio", desde, "fechaFin", hasta, "edadMin", edadMin,
                    "edadMax", edadMax, "plazas", plazas, "horario", horario));
        }

        private int registrarMenor(String tutor, String nombre, String nacimiento) throws Exception {
            return id(enviar(post("/api/menores"), tutor, menor(nombre, nacimiento, true))
                    .andExpect(status().isCreated()), "$.id");
        }

        private Map<String, Object> menor(String nombre, String nacimiento, boolean consentimiento) {
            return Map.of("nombre", nombre, "apellidos", "Prueba", "fechaNacimiento", nacimiento,
                    "telefonoTutor", "600222333", "contactoEmergenciaNombre", "Abuela Prueba",
                    "contactoEmergenciaTelefono", "600444555", "consentimientoRgpd", consentimiento);
        }

        private ResultActions inscribir(String tutor, int escuela, int menor) throws Exception {
            return enviar(post("/api/escuelas/{id}/inscripciones", escuela), tutor, Map.of("idMenor", menor));
        }

        @Test
        void el_catalogo_muestra_horario_y_plazas_y_la_escuela_bloquea_su_instalacion() throws Exception {
            enviar(get("/api/escuelas"), TUTORA, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nombre").value("Escuela de natación infantil"))
                    .andExpect(jsonPath("$[0].horario", hasSize(2)))
                    .andExpect(jsonPath("$[0].inscritos").value(1))
                    .andExpect(jsonPath("$[0].plazasLibres").value(11));

            assertThat(estadoFranja(INST_PISCINA_2, "2026-11-02", "17:00")).isEqualTo("BLOQUEADA"); // lunes
            assertThat(estadoFranja(INST_PISCINA_2, "2026-11-03", "17:00")).isEqualTo("LIBRE");     // martes
        }

        @Test
        void P41_no_se_crea_la_escuela_si_hay_reservas_confirmadas_y_P42_los_festivos_se_omiten() throws Exception {
            int reserva = id(enviar(post("/api/reservas"), PDI,
                    Map.of("idInstalacion", INST_TENIS_1, "fecha", "2026-12-01", "horaInicio", "17:00"))
                    .andExpect(status().isCreated()), "$.id");
            List<Map<String, Object>> martes = List.of(Map.of("diaSemana", 2, "horaInicio", "17:00", "horaFin", "18:00"));

            crearEscuela("Escuela de tenis", INST_TENIS_1, "2026-11-30", "2026-12-13", 8, 14, 10, martes)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ESCUELA_CON_RESERVAS"));
            assertThat(estadoReserva(reserva)).isEqualTo("CONFIRMADA");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM escuela WHERE nombre = 'Escuela de tenis'",
                    Integer.class)).isZero();

            // Resuelta la planificación, se crea: martes 1/12 sí, martes 8/12 es festivo y se omite
            enviar(patch("/api/reservas/{id}/cancelar", reserva), PDI, null).andExpect(status().isOk());
            crearEscuela("Escuela de tenis", INST_TENIS_1, "2026-11-30", "2026-12-13", 8, 14, 10, martes)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sesionesBloqueadas").value(1))
                    .andExpect(jsonPath("$.sesionesOmitidas").value(1))
                    .andExpect(jsonPath("$.escuela.diasHorario", containsString("17:00-18:00")));
            assertThat(estadoFranja(INST_TENIS_1, "2026-12-01", "17:00")).isEqualTo("BLOQUEADA");
        }

        @Test
        void P55_una_reserva_institucional_pendiente_tambien_impide_crear_la_escuela() throws Exception {
            // Seed: institucional PENDIENTE en el fútbol 7 el jueves 29/10 de 19 a 21
            crearEscuela("Escuela de fútbol", INST_FUTBOL, "2026-10-26", "2026-11-01", 8, 14, 20,
                    List.of(Map.of("diaSemana", 4, "horaInicio", "19:00", "horaFin", "20:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ESCUELA_CON_RESERVAS"));
            assertThat(estadoReserva(RESERVA_INSTITUCIONAL_PENDIENTE)).isEqualTo("PENDIENTE");
        }

        @Test
        void P56_la_administracion_cancela_una_escuela_y_libera_sus_sesiones() throws Exception {
            int escuela = id(crearEscuela("Escuela a cancelar", INST_TENIS_2, "2026-11-02", "2026-11-30", 6, 12, 5,
                    List.of(Map.of("diaSemana", 3, "horaInicio", "17:00", "horaFin", "18:00")))
                    .andExpect(status().isCreated()), "$.escuela.id");
            inscribir(TUTORA, escuela, MENOR_HUGO).andExpect(status().isCreated());
            assertThat(estadoFranja(INST_TENIS_2, "2026-11-04", "17:00")).isEqualTo("BLOQUEADA");

            enviar(patch("/api/admin/escuelas/{id}/cancelar", escuela), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escuela.estado").value("CANCELADA"))
                    .andExpect(jsonPath("$.reservasCanceladas").value(1));
            assertThat(estadoFranja(INST_TENIS_2, "2026-11-04", "17:00")).isEqualTo("LIBRE");
            enviar(get("/api/escuelas"), TUTORA, null).andExpect(jsonPath("$[*].id", not(hasItem(escuela))));
            inscribir(TUTORA, escuela, MENOR_HUGO)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ESCUELA_CANCELADA"));
        }

        @Test
        void P52_el_tutor_corrige_el_contacto_del_menor() throws Exception {
            enviar(patch("/api/menores/{id}/contacto", MENOR_HUGO), TUTORA, Map.of("telefonoTutor", "611000000",
                    "contactoEmergenciaNombre", "Elena Campos (tía)", "contactoEmergenciaTelefono", "622000000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.telefonoTutor").value("611000000"))
                    .andExpect(jsonPath("$.contactoEmergenciaNombre").value("Elena Campos (tía)"));
            enviar(patch("/api/menores/{id}/contacto", MENOR_HUGO), REPRESENTANTE, Map.of("telefonoTutor", "1",
                    "contactoEmergenciaNombre", "x", "contactoEmergenciaTelefono", "2"))
                    .andExpect(status().isNotFound());
            jdbc.update("UPDATE menor SET telefono_tutor = '600000006', contacto_emergencia_nombre = 'Antonio Ruiz Gómez (abuelo)', "
                    + "contacto_emergencia_telefono = '600000007' WHERE id_menor = ?", MENOR_HUGO);
        }

        @Test
        void P42_un_cierre_posterior_cancela_la_sesion_de_la_escuela() throws Exception {
            assertThat(estadoFranja(INST_PISCINA_2, "2026-11-02", "17:00")).isEqualTo("BLOQUEADA");
            enviar(post("/api/admin/cierres"), ADMIN, Map.of("inicio", "2026-11-02T00:00:00",
                    "fin", "2026-11-03T00:00:00", "observaciones", "Cierre extraordinario"))
                    .andExpect(status().isCreated());
            List<String> motivos = JsonPath.read(enviar(get("/api/instalaciones/{id}/disponibilidad", INST_PISCINA_2)
                            .param("fecha", "2026-11-02"), ALUMNA, null).andReturn().getResponse().getContentAsString(),
                    "$.franjas[*].motivo");
            assertThat(motivos).containsOnly("CIERRE");
        }

        @Test
        void se_rechazan_horarios_solapados_y_tramos_incoherentes() throws Exception {
            crearEscuela("Escuela mala", INST_TENIS_2, "2026-11-30", "2026-12-13", 8, 14, 10,
                    List.of(Map.of("diaSemana", 2, "horaInicio", "17:00", "horaFin", "18:30"),
                            Map.of("diaSemana", 2, "horaInicio", "18:00", "horaFin", "19:00")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("HORARIO_NO_VALIDO"));
            crearEscuela("Escuela mala", INST_TENIS_2, "2026-11-30", "2026-12-13", 14, 8, 10,
                    List.of(Map.of("diaSemana", 2, "horaInicio", "17:00", "horaFin", "18:00")))
                    .andExpect(jsonPath("$.codigo").value("EDADES_NO_VALIDAS"));
        }

        @Test
        void P22_P44_sin_consentimiento_no_se_registra_y_se_guardan_los_contactos() throws Exception {
            enviar(post("/api/menores"), TUTORA, menor("Ana", "2018-01-10", false))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.consentimientoRgpd").exists());

            Map<String, Object> sinEmergencia = new java.util.HashMap<>(menor("Ana", "2018-01-10", true));
            sinEmergencia.remove("contactoEmergenciaNombre");
            enviar(post("/api/menores"), TUTORA, sinEmergencia)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.contactoEmergenciaNombre").exists());

            enviar(post("/api/menores"), TUTORA, menor("Ana", "2018-01-10", true))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fechaConsentimiento").value("2026-10-23T10:00:00"))
                    .andExpect(jsonPath("$.telefonoTutor").value("600222333"))
                    .andExpect(jsonPath("$.contactoEmergenciaNombre").value("Abuela Prueba"));
        }

        @Test
        void P45_al_retirar_el_consentimiento_se_eliminan_los_datos_y_las_inscripciones() throws Exception {
            int id = registrarMenor(TUTORA, "Leo", "2018-01-10");
            inscribir(TUTORA, ESCUELA_NATACION, id).andExpect(status().isCreated());

            enviar(delete("/api/menores/{id}", id), TUTORA, null).andExpect(status().isNoContent());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM menor WHERE id_menor = ?", Integer.class, id)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM inscripcion_escuela WHERE id_menor = ?", Integer.class, id))
                    .isZero();
            enviar(delete("/api/menores/{id}", MENOR_HUGO), REPRESENTANTE, null).andExpect(status().isNotFound());
        }

        @Test
        void P20_la_edad_del_menor_debe_estar_en_el_tramo_a_la_fecha_de_inscripcion() throws Exception {
            int pequena = registrarMenor(TUTORA, "Lola", "2022-05-01");        // 4 años
            inscribir(TUTORA, ESCUELA_NATACION, pequena)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("EDAD_FUERA_DE_TRAMO"));

            int mayor = registrarMenor(TUTORA, "Leo", "2018-01-10");           // 8 años
            inscribir(TUTORA, ESCUELA_NATACION, mayor)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.inscripciones[0].estado").value("ACTIVA"));
        }

        @Test
        void un_menor_no_se_inscribe_dos_veces_y_la_baja_libera_la_plaza() throws Exception {
            inscribir(TUTORA, ESCUELA_NATACION, MENOR_HUGO)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("YA_INSCRITO"));

            enviar(patch("/api/escuelas/{id}/inscripciones/{m}/baja", ESCUELA_NATACION, MENOR_HUGO), TUTORA, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inscripciones[0].estado").value("BAJA"));
            inscribir(TUTORA, ESCUELA_NATACION, MENOR_HUGO).andExpect(status().isCreated());
        }

        @Test
        void no_se_superan_las_plazas() throws Exception {
            int escuela = id(crearEscuela("Escuela mini", INST_TENIS_2, "2026-11-30", "2026-12-13", 6, 12, 1,
                    List.of(Map.of("diaSemana", 4, "horaInicio", "17:00", "horaFin", "18:00")))
                    .andExpect(status().isCreated()), "$.escuela.id");
            inscribir(TUTORA, escuela, MENOR_HUGO).andExpect(status().isCreated());
            int otro = registrarMenor(TUTORA, "Leo", "2018-01-10");
            inscribir(TUTORA, escuela, otro)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("SIN_PLAZAS"));
        }

        @Test
        void cada_tutor_gestiona_sus_menores_y_P39_la_comunidad_UAL_tambien_puede_ser_tutora() throws Exception {
            inscribir(REPRESENTANTE, ESCUELA_NATACION, MENOR_HUGO)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("MENOR_NO_ENCONTRADO"));
            int hijo = registrarMenor(PDI, "Mateo", "2016-06-01");
            inscribir(PDI, ESCUELA_NATACION, hijo).andExpect(status().isCreated());
            enviar(post("/api/menores"), ADMIN, menor("Ana", "2018-01-10", true))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_NO_AUTORIZADO"));
            enviar(get("/api/menores"), TUTORA, null)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].inscripciones[0].escuela").value("Escuela de natación infantil"));
        }

        @Test
        void la_administracion_ve_los_inscritos_con_el_contacto_del_tutor() throws Exception {
            enviar(get("/api/admin/escuelas/{id}/inscritos", ESCUELA_NATACION), ADMIN, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].menor").value("Hugo Ruiz Campos"))
                    .andExpect(jsonPath("$[0].emailTutor").value(TUTORA))
                    .andExpect(jsonPath("$[0].telefonoTutor").value("600000006"));
            enviar(get("/api/admin/escuelas"), TUTORA, null).andExpect(status().isForbidden());
        }
    }
}
