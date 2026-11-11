package pe.agrodirecto.notificaciones.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.notificaciones.servicio.NotificacionService;

@RestController
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService servicio;

    /** Campana de la web: avisos del usuario y cuántos faltan leer. */
    @GetMapping("/api/notificaciones")
    public NotificacionService.BandejaResponse bandeja(@AuthenticationPrincipal Jwt jwt) {
        return servicio.bandeja(jwt);
    }

    @PostMapping("/api/notificaciones/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        servicio.marcarLeida(jwt, id);
    }

    @PostMapping("/api/notificaciones/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(@AuthenticationPrincipal Jwt jwt) {
        servicio.marcarTodasLeidas(jwt);
    }

    /** SMS y WhatsApp enviados a un agricultor, con el enlace wa.me para reenviar. */
    @GetMapping("/api/gestion/mensajes")
    public List<NotificacionResponse> mensajesDelProductor(@AuthenticationPrincipal Jwt jwt,
            @RequestParam Long productorId) {
        return servicio.mensajesDelProductor(jwt, productorId);
    }
}
