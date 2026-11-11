package pe.agrodirecto.notificaciones.servicio;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.Texto;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.notificaciones.dominio.CanalNotificacion;
import pe.agrodirecto.notificaciones.dominio.Notificacion;
import pe.agrodirecto.notificaciones.repositorio.NotificacionRepository;
import pe.agrodirecto.notificaciones.web.NotificacionResponse;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.servicio.ProductorService;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final String PREFIJO_PAIS = "51";

    private final NotificacionRepository notificaciones;
    private final EnvioSms envioSms;
    private final UsuarioActualService usuarioActual;
    private final ProductorService productorService;
    private final Clock reloj;

    /**
     * RN-B2: el agricultor se entera por SMS (llega a cualquier celular) y además queda un enlace de WhatsApp
     * listo para que su agente lo reenvíe gratis. Si tiene cuenta propia o un agente, también reciben el aviso web.
     */
    @Transactional
    public void avisarAlProductor(Productor productor, Long pedidoId, String mensaje) {
        String sms = Texto.sinTildes(mensaje);
        envioSms.enviar(productor.getTelefono(), sms);
        guardar(productor.getId(), null, pedidoId, CanalNotificacion.SMS, productor.getTelefono(), sms, null);
        guardar(productor.getId(), null, pedidoId, CanalNotificacion.WHATSAPP, productor.getTelefono(), mensaje,
                enlaceWhatsapp(productor.getTelefono(), mensaje));
        if (productor.getUsuario() != null) {
            avisarAlUsuario(productor.getUsuario(), pedidoId, mensaje);
        }
        if (productor.getAgente() != null) {
            avisarAlUsuario(productor.getAgente(), pedidoId, "[" + productor.getNombreComercial() + "] " + mensaje);
        }
    }

    @Transactional
    public void avisarAlUsuario(Usuario usuario, Long pedidoId, String mensaje) {
        guardar(null, usuario.getId(), pedidoId, CanalNotificacion.WEB, null, mensaje, null);
    }

    /** Respuesta automática por SMS al agricultor (confirmaciones y ayuda). */
    @Transactional
    public void responderSms(String celular, Long productorId, String mensaje) {
        String sms = Texto.sinTildes(mensaje);
        envioSms.enviar(celular, sms);
        guardar(productorId, null, null, CanalNotificacion.SMS, celular, sms, null);
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public BandejaResponse bandeja(Jwt jwt) {
        Usuario usuario = usuarioActual.obtener(jwt);
        List<NotificacionResponse> lista = notificaciones
                .findByUsuarioIdAndCanalOrderByCreadaEnDescIdDesc(usuario.getId(), CanalNotificacion.WEB).stream()
                .map(NotificacionResponse::de).toList();
        return new BandejaResponse(
                notificaciones.countByUsuarioIdAndCanalAndLeidaFalse(usuario.getId(), CanalNotificacion.WEB), lista);
    }

    @Transactional
    public void marcarLeida(Jwt jwt, Long id) {
        Usuario usuario = usuarioActual.obtener(jwt);
        notificaciones.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("Notificación", id))
                .setLeida(true);
    }

    @Transactional
    public void marcarTodasLeidas(Jwt jwt) {
        Usuario usuario = usuarioActual.obtener(jwt);
        notificaciones.findByUsuarioIdAndCanalOrderByCreadaEnDescIdDesc(usuario.getId(), CanalNotificacion.WEB)
                .forEach(n -> n.setLeida(true));
    }

    /** Mensajes enviados a un agricultor: el agente ve qué le llegó y puede reenviarlo por WhatsApp. */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> mensajesDelProductor(Jwt jwt, Long productorId) {
        productorService.buscarGestionable(jwt, productorId);
        return notificaciones.findByProductorIdAndCanalInOrderByCreadaEnDescIdDesc(productorId,
                List.of(CanalNotificacion.SMS, CanalNotificacion.WHATSAPP)).stream()
                .map(NotificacionResponse::de).toList();
    }

    public record BandejaResponse(long noLeidas, List<NotificacionResponse> notificaciones) {
    }

    // ---------- Internos ----------

    /** Enlace "click to chat" de WhatsApp: gratuito, no requiere la API de pago. */
    static String enlaceWhatsapp(String celular, String mensaje) {
        return "https://wa.me/" + PREFIJO_PAIS + celular + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void guardar(Long productorId, Long usuarioId, Long pedidoId, CanalNotificacion canal, String destino,
            String mensaje, String enlace) {
        Notificacion n = new Notificacion();
        n.setProductorId(productorId);
        n.setUsuarioId(usuarioId);
        n.setPedidoId(pedidoId);
        n.setCanal(canal);
        n.setDestino(destino);
        n.setMensaje(mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje);
        n.setEnlace(enlace);
        n.setCreadaEn(LocalDateTime.now(reloj));
        notificaciones.save(n);
    }
}
