package pe.agrodirecto.tiemporeal.config;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.config.SeguridadConfig;
import pe.agrodirecto.tiemporeal.servicio.PermisosTiempoReal;

/**
 * Seguridad de los mensajes STOMP.
 * <ul>
 * <li>CONNECT: si llega {@code Authorization: Bearer <jwt>} se valida con la misma clave que la API REST.
 * Sin token la conexión es anónima y solo sirve para canales públicos.</li>
 * <li>SUBSCRIBE: cada canal exige su permiso (ver {@link Destinos}).</li>
 * <li>SEND: los clientes solo escuchan; no se aceptan mensajes entrantes.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AutenticacionStomp implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final PermisosTiempoReal permisos;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (stomp == null || stomp.getCommand() == null) {
            return message;
        }
        switch (stomp.getCommand()) {
            case CONNECT -> autenticar(stomp);
            case SUBSCRIBE -> autorizar(stomp.getDestination(), stomp.getUser());
            case SEND -> throw new MessageDeliveryException("Este canal es solo de lectura.");
            default -> {
            }
        }
        return message;
    }

    private void autenticar(StompHeaderAccessor stomp) {
        String cabecera = stomp.getFirstNativeHeader("Authorization");
        if (cabecera == null || cabecera.isBlank()) {
            return;
        }
        if (!cabecera.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Cabecera Authorization inválida.");
        }
        try {
            Jwt jwt = jwtDecoder.decode(cabecera.substring(7));
            if (!permisos.cuentaActiva(Long.valueOf(jwt.getSubject()))) {
                throw new MessageDeliveryException("La cuenta no está activa.");
            }
            // El nombre del usuario en STOMP es su id: así se le envían mensajes privados.
            stomp.setUser(SeguridadConfig.convertidorRoles().convert(jwt));
        } catch (JwtException e) {
            throw new MessageDeliveryException("Token inválido o expirado.");
        }
    }

    private void autorizar(String destino, Principal usuario) {
        if (Destinos.esStockPublico(destino)) {
            return;
        }
        if (usuario == null) {
            throw new MessageDeliveryException("Inicia sesión para suscribirte a " + destino + ".");
        }
        if (Destinos.MIS_PEDIDOS_SUSCRIPCION.equals(destino)) {
            return;
        }
        Long productorId = Destinos.productorDe(destino)
                .orElseThrow(() -> new MessageDeliveryException("Canal desconocido: " + destino));
        if (!permisos.puedeEscucharProductor(Long.valueOf(usuario.getName()), productorId)) {
            throw new MessageDeliveryException("No gestionas al productor " + productorId + ".");
        }
    }
}
