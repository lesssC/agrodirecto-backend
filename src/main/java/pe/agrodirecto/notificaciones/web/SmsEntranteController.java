package pe.agrodirecto.notificaciones.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.NoAutenticadoException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.notificaciones.servicio.RespuestaSmsService;

/**
 * Webhook que llamaría el proveedor de SMS al recibir la respuesta de un agricultor.
 * Se autentica con un secreto compartido en la cabecera {@code X-Webhook-Secreto}.
 */
@RestController
@RequiredArgsConstructor
public class SmsEntranteController {

    private final RespuestaSmsService servicio;
    private final AgroDirectoProperties propiedades;

    @PostMapping("/api/sms/entrante")
    public RespuestaSmsService.Resultado recibir(
            @RequestHeader(name = "X-Webhook-Secreto", required = false) String secreto,
            @Valid @RequestBody SmsEntranteRequest solicitud) {
        if (!secretoValido(secreto)) {
            throw new NoAutenticadoException("Secreto del webhook inválido.");
        }
        return servicio.procesar(solicitud.telefono().replaceAll("\\D", "").replaceFirst("^51(?=9\\d{8}$)", ""),
                solicitud.mensaje());
    }

    /** Comparación en tiempo constante para no filtrar el secreto por diferencias de tiempo de respuesta. */
    private boolean secretoValido(String recibido) {
        return recibido != null && MessageDigest.isEqual(recibido.getBytes(StandardCharsets.UTF_8),
                propiedades.secretoWebhookSms().getBytes(StandardCharsets.UTF_8));
    }
}
