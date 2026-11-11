package pe.agrodirecto.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AgroDirectoProperties.class)
public class AplicacionConfig {

    public static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    /** Reloj inyectable: permite simular el paso del tiempo en las pruebas. */
    @Bean
    Clock reloj() {
        return Clock.system(ZONA_PERU);
    }
}
