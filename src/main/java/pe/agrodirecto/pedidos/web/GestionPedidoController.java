package pe.agrodirecto.pedidos.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.servicio.PedidoService;

/** Pedidos recibidos: los atiende el agricultor, su agente de campo o la administración. */
@RestController
@RequestMapping("/api/gestion/pedidos")
@RequiredArgsConstructor
public class GestionPedidoController {

    private final PedidoService servicio;

    @GetMapping
    public List<PedidoResponse> recibidos(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long productorId, @RequestParam(required = false) EstadoPedido estado) {
        return servicio.recibidos(jwt, productorId, estado);
    }

    @PostMapping("/{id}/aceptar")
    public PedidoResponse aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.aceptar(jwt, id);
    }

    @PostMapping("/{id}/rechazar")
    public PedidoResponse rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest solicitud) {
        return servicio.rechazar(jwt, id, solicitud == null ? null : solicitud.motivo());
    }

    @PostMapping("/{id}/estado")
    public PedidoResponse avanzar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest solicitud) {
        return servicio.avanzar(jwt, id, solicitud.estado(), solicitud.comentario());
    }
}
