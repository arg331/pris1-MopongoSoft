package es.ual.sadual.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: PostgreSQL 16 real en Docker, inicializado con
 * los MISMOS scripts del repositorio (database/01_schema.sql y 02_seed.sql). Así se
 * prueban también la restricción EXCLUDE, los triggers y fn_franjas_disponibles.
 * <p>
 * Un único contenedor para todas las clases (patrón "singleton container") y un reloj
 * fijo: "ahora" es el viernes 23/10/2026 a las 10:00, de modo que las reservas del seed
 * (26–29/10) son siempre futuras, se ejecuten las pruebas el día que se ejecuten.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(AbstractIntegracionIT.RelojFijo.class)
public abstract class AbstractIntegracionIT {

    protected static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    protected static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 23, 10, 0);
    protected static final String PASSWORD_DEMO = "Sadual2026!";

    // Ids que genera 02_seed.sql sobre una base de datos recién creada
    protected static final int INST_PADEL_1 = 1, INST_PADEL_2 = 2, INST_TENIS_1 = 3, INST_TENIS_2 = 4,
            INST_PABELLON = 6, INST_SALA = 9;
    protected static final int RESERVA_ALUMNA_PADEL = 1;
    /** Variación 7 en el seed: escuela de natación (calle 2 de la piscina, L y X 17-18) y evento del 19/12. */
    protected static final int INST_PISCINA_2 = 8, INST_FUTBOL = 5;
    protected static final int ESCUELA_NATACION = 1, MENOR_HUGO = 1, EVENTO_PUERTAS_ABIERTAS = 1;
    protected static final int RESERVA_INSTITUCIONAL_PENDIENTE = 4;

    /** Último id de cada tabla tras el seed: lo que pase de ahí lo han creado las pruebas. */
    private static Map<String, Integer> ultimosIdsDelSeed;

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sadual")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("..", "database", "01_schema.sql")),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("..", "database", "02_seed.sql")),
                    "/docker-entrypoint-initdb.d/02_seed.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA.atZone(MADRID).toInstant(), MADRID);
        }
    }

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void anotarIdsDelSeed() {
        synchronized (AbstractIntegracionIT.class) {
            if (ultimosIdsDelSeed == null) {
                Map<String, Integer> ids = new HashMap<>();
                for (String[] t : new String[][] {{"bloqueo", "id_bloqueo"}, {"usuario", "id_usuario"},
                        {"evento", "id_evento"}, {"escuela", "id_escuela"}, {"menor", "id_menor"},
                        {"instalacion", "id_instalacion"}, {"entidad_convenio", "id_entidad"}}) {
                    ids.put(t[0], jdbc.queryForObject("SELECT coalesce(max(" + t[1] + "), 0) FROM " + t[0], Integer.class));
                }
                ultimosIdsDelSeed = Map.copyOf(ids);
            }
        }
    }

    /** Deja la base de datos como la dejó el seed, para que las pruebas sean independientes. */
    @AfterEach
    void restaurarDatosDelSeed() {
        Map<String, Integer> seed = ultimosIdsDelSeed;
        jdbc.update("DELETE FROM sancion");
        // Variación 7: inscripciones, menores, escuelas y eventos creados por las pruebas
        jdbc.update("DELETE FROM inscripcion_escuela WHERE NOT (id_escuela = ? AND id_menor = ?)",
                ESCUELA_NATACION, MENOR_HUGO);
        jdbc.update("UPDATE inscripcion_escuela SET estado = 'ACTIVA' WHERE estado <> 'ACTIVA'");
        jdbc.update("DELETE FROM menor WHERE id_menor > ?", seed.get("menor"));
        jdbc.update("DELETE FROM escuela WHERE id_escuela > ?", seed.get("escuela"));
        jdbc.update("UPDATE escuela SET estado = 'ACTIVA' WHERE estado <> 'ACTIVA'");
        jdbc.update("DELETE FROM inscripcion_evento WHERE id_evento > ? OR id_usuario <> 2", seed.get("evento"));
        jdbc.update("UPDATE inscripcion_evento SET estado = 'ACTIVA' WHERE estado <> 'ACTIVA'");
        jdbc.update("DELETE FROM evento WHERE id_evento > ?", seed.get("evento"));
        jdbc.update("UPDATE evento SET estado = 'PUBLICADO' WHERE id_evento = ?", EVENTO_PUERTAS_ABIERTAS);
        jdbc.update("DELETE FROM bloqueo WHERE id_bloqueo > ?", seed.get("bloqueo"));
        jdbc.update("DELETE FROM reserva WHERE id_reserva > 4");
        jdbc.update("""
                UPDATE reserva
                   SET estado = CASE WHEN id_reserva = 4 THEN 'PENDIENTE' ELSE 'CONFIRMADA' END,
                       fecha_cancelacion = NULL, id_autorizador = NULL, fecha_resolucion = NULL, motivo_rechazo = NULL
                 WHERE id_reserva <= 4
                """);
        jdbc.update("DELETE FROM instalacion WHERE id_instalacion > ?", seed.get("instalacion"));
        jdbc.update("DELETE FROM usuario WHERE id_usuario > ?", seed.get("usuario"));
        jdbc.update("DELETE FROM entidad_convenio WHERE id_entidad > ?", seed.get("entidad_convenio"));
        jdbc.update("UPDATE entidad_convenio SET suspendida_hasta = NULL, motivo_suspension = NULL WHERE suspendida_hasta IS NOT NULL");
        jdbc.update("""
                UPDATE usuario u SET id_rol = r.id_rol
                  FROM rol r
                 WHERE r.codigo = CASE u.email WHEN 'admin.deportes@ual.es' THEN 'ADMIN'
                                               WHEN 'alumna.demo@ual.es' THEN 'ALUMNADO'
                                               WHEN 'pdi.demo@ual.es' THEN 'PDI'
                                               WHEN 'pas.demo@ual.es' THEN 'PAS' ELSE 'EXTERNO' END
                   AND u.id_rol <> r.id_rol
                """);
        jdbc.update("UPDATE usuario SET activo = TRUE WHERE NOT activo");
        jdbc.update("UPDATE instalacion SET activa = TRUE WHERE NOT activa");
    }

    protected String token(String email) {
        return TOKENS.computeIfAbsent(email, this::login);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    private String login(String email) {
        try {
            String cuerpo = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD_DEMO + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.read(cuerpo, "$.token");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar sesión como " + email, e);
        }
    }
}
