package pe.agrodirecto.notificaciones.web;

import java.time.LocalDateTime;

import pe.agrodirecto.notificaciones.dominio.CanalNotificacion;
import pe.agrodirecto.notificaciones.dominio.Notificacion;

public record NotificacionResponse(Long id, CanalNotificacion canal, String destino, String mensaje, String enlace,
        Long pedidoId, boolean leida, LocalDateTime creadaEn) {

    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getCanal(), n.getDestino(), n.getMensaje(), n.getEnlace(),
                n.getPedidoId(), n.isLeida(), n.getCreadaEn());
    }
}
