package pe.agrodirecto.pedidos.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.servicio.PedidoService;

/** Pedidos desde el lado del comprador. El detalle lo puede ver también quien gestiona al productor. */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService servicio;

    @PostMapping("/cotizacion")
    public CotizacionResponse cotizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PedidoRequest solicitud) {
        return servicio.cotizar(jwt, solicitud);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PedidoRequest solicitud) {
        return servicio.crear(jwt, solicitud);
    }

    @GetMapping
    public List<PedidoResponse> misPedidos(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) EstadoPedido estado) {
        return servicio.misPedidos(jwt, estado);
    }

    @GetMapping("/{id}")
    public PedidoResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.detalle(jwt, id);
    }

    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest solicitud) {
        return servicio.cancelar(jwt, id, solicitud == null ? null : solicitud.motivo());
    }

    @PostMapping("/{id}/confirmar-recepcion")
    public PedidoResponse confirmarRecepcion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.confirmarRecepcion(jwt, id);
    }
}
