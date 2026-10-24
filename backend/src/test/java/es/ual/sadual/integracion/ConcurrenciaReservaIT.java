package es.ual.sadual.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criterio de aceptación de G2 / riesgo R5: "cero dobles reservas en las pruebas de concurrencia".
 * Peticiones HTTP reales y simultáneas contra el servidor arrancado: solo una puede ganar.
 */
@DisplayName("F04 · Cero dobles reservas (P10) y una reserva por día (P1) con peticiones simultáneas")
class ConcurrenciaReservaIT extends AbstractIntegracionIT {

    private static final int PETICIONES = 10;
    // Perfiles que pueden reservar (P4). Con 10 peticiones, cada usuario lanza varias a la vez.
    private static final List<String> USUARIOS = List.of("alumna.demo@ual.es", "pdi.demo@ual.es",
            "pas.demo@ual.es", "admin.deportes@ual.es");

    @Autowired
    private TestRestTemplate http;

    @RepeatedTest(20)
    void solo_una_de_diez_peticiones_simultaneas_consigue_la_franja() throws Exception {
        String cuerpo = "{\"idInstalacion\":" + INST_TENIS_2 + ",\"fecha\":\"2026-10-30\",\"horaInicio\":\"18:00\"}";
        List<String> tokens = USUARIOS.stream().map(this::token).toList();

        ExecutorService hilos = Executors.newFixedThreadPool(PETICIONES);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> respuestas = new ArrayList<>();
        try {
            for (int i = 0; i < PETICIONES; i++) {
                String token = tokens.get(i % tokens.size());
                respuestas.add(hilos.submit(() -> {
                    HttpHeaders cabeceras = new HttpHeaders();
                    cabeceras.setBearerAuth(token);
                    cabeceras.setContentType(MediaType.APPLICATION_JSON);
                    salida.await(); // todas salen a la vez
                    return http.exchange("/api/reservas", HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras), String.class);
                }));
            }
            salida.countDown();

            List<String> resultados = resultados(respuestas);

            // Una gana y las demás reciben "franja ocupada": ni 500 ni deadlocks (40P01)
            assertThat(resultados).filteredOn("201"::equals).hasSize(1);
            assertThat(resultados).filteredOn("409 FRANJA_OCUPADA"::equals).hasSize(PETICIONES - 1);
        } finally {
            hilos.shutdownNow();
        }

        Integer activas = jdbc.queryForObject("""
                SELECT count(*) FROM reserva
                 WHERE id_instalacion = ? AND fecha = DATE '2026-10-30' AND hora_inicio = TIME '18:00'
                   AND estado IN ('PENDIENTE','CONFIRMADA')
                """, Integer.class, INST_TENIS_2);
        assertThat(activas).isEqualTo(1);
    }

    /** P1 bajo carga: la misma persona pide 5 franjas distintas del mismo día a la vez → solo 1. */
    @RepeatedTest(5)
    void una_persona_no_consigue_dos_reservas_el_mismo_dia_aunque_las_pida_a_la_vez() throws Exception {
        String token = token("pas.demo@ual.es");
        List<String> horas = List.of("09:00", "10:00", "11:00", "12:00", "13:00");

        ExecutorService hilos = Executors.newFixedThreadPool(horas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> respuestas = new ArrayList<>();
        try {
            for (String hora : horas) {
                String cuerpo = "{\"idInstalacion\":" + INST_TENIS_1 + ",\"fecha\":\"2026-10-30\",\"horaInicio\":\"" + hora + "\"}";
                respuestas.add(hilos.submit(() -> {
                    HttpHeaders cabeceras = new HttpHeaders();
                    cabeceras.setBearerAuth(token);
                    cabeceras.setContentType(MediaType.APPLICATION_JSON);
                    salida.await();
                    return http.exchange("/api/reservas", HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras), String.class);
                }));
            }
            salida.countDown();
            List<String> resultados = resultados(respuestas);
            assertThat(resultados).filteredOn("201"::equals).hasSize(1);
            assertThat(resultados).filteredOn("409 LIMITE_DIARIO"::equals).hasSize(horas.size() - 1);
        } finally {
            hilos.shutdownNow();
        }
    }

    /** "201" o "409 FRANJA_OCUPADA": estado HTTP más el codigo de error, para que un 500 no pase desapercibido. */
    private static List<String> resultados(List<Future<ResponseEntity<String>>> respuestas) throws Exception {
        List<String> resultados = new ArrayList<>();
        for (Future<ResponseEntity<String>> r : respuestas) {
            ResponseEntity<String> respuesta = r.get(30, TimeUnit.SECONDS);
            int estado = respuesta.getStatusCode().value();
            resultados.add(estado == 201 ? "201" : estado + " " + JsonPath.read(respuesta.getBody(), "$.codigo"));
        }
        return resultados;
    }
}
