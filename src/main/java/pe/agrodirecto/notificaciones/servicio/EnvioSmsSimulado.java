package pe.agrodirecto.notificaciones.servicio;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Registra el SMS en el log en lugar de enviarlo: no tiene costo y sirve para la demostración. */
@Slf4j
@Component
public class EnvioSmsSimulado implements EnvioSms {

    @Override
    public void enviar(String celular, String texto) {
        log.info("[SMS simulado] a {}: {}", celular, texto);
    }
}
