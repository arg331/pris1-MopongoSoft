package es.ual.sadual.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de negocio inyectable: las reglas "no reservar en el pasado" o "no cancelar una
 * reserva ya empezada" dependen de la hora, y así las pruebas pueden fijarla.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock reloj(@Value("${sadual.zona-horaria:Europe/Madrid}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
