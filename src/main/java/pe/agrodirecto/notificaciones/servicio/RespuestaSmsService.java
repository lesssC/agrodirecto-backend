package pe.agrodirecto.notificaciones.servicio;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.Texto;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.repositorio.PedidoRepository;
import pe.agrodirecto.pedidos.servicio.PedidoService;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.repositorio.ProductorRepository;

/**
 * RN-B2: el agricultor con un celular básico atiende sus pedidos por SMS.
 * Entiende "1" / "SI" / "ACEPTO" para aceptar y "2" / "NO" / "RECHAZO" para rechazar,
 * con o sin el código del pedido ("1 AG-2501"). Sin código, actúa sobre el único pedido pendiente.
 */
@Service
@RequiredArgsConstructor
public class RespuestaSmsService {

    private static final Pattern ACEPTAR = Pattern.compile("^(1|si|acepto|aceptar)\\b\\s*(ag\\s*-?\\s*\\d+)?\\s*$");
    private static final Pattern RECHAZAR = Pattern.compile("^(2|no|rechazo|rechazar)\\b\\s*(ag\\s*-?\\s*\\d+)?\\s*$");

    private final ProductorRepository productores;
    private final PedidoRepository pedidos;
    private final PedidoService pedidoService;
    private final NotificacionService notificaciones;

    public record Resultado(String respuesta, String pedido, String accion) {
    }

    @Transactional
    public Resultado procesar(String celular, String mensaje) {
        List<Productor> remitentes = productores.findByTelefono(celular);
        if (remitentes.isEmpty()) {
            return responder(celular, null, "Este número no está registrado en AgroDirecto. Pide ayuda a tu agente de campo.",
                    null, null);
        }
        Long productorId = remitentes.getFirst().getId();
        String texto = Texto.normalizar(mensaje);
        Matcher aceptar = ACEPTAR.matcher(texto);
        Matcher rechazar = RECHAZAR.matcher(texto);
        boolean acepta = aceptar.matches();
        if (!acepta && !rechazar.matches()) {
            return responder(celular, productorId, "No entendimos tu mensaje. Responde 1 y el código para ACEPTAR "
                    + "(ejemplo: 1 AG-2501) o 2 y el código para RECHAZAR.", null, null);
        }
        String codigo = codigo(acepta ? aceptar.group(2) : rechazar.group(2));

        List<Pedido> pendientes = pedidos.findByProductorIdInAndEstadoOrderByCreadoEn(
                remitentes.stream().map(Productor::getId).toList(), EstadoPedido.PENDIENTE);
        Optional<Pedido> elegido;
        if (codigo != null) {
            elegido = pendientes.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
            if (elegido.isEmpty()) {
                return responder(celular, productorId, "No tienes un pedido " + codigo + " por responder.", codigo, null);
            }
        } else if (pendientes.isEmpty()) {
            return responder(celular, productorId, "No tienes pedidos por responder.", null, null);
        } else if (pendientes.size() > 1) {
            String codigos = String.join(", ", pendientes.stream().map(Pedido::getCodigo).toList());
            return responder(celular, productorId, "Tienes " + pendientes.size() + " pedidos por responder: " + codigos
                    + ". Responde 1 y el código, por ejemplo: 1 " + pendientes.getFirst().getCodigo() + ".", null, null);
        } else {
            elegido = Optional.of(pendientes.getFirst());
        }

        Pedido pedido = elegido.get();
        if (acepta) {
            pedidoService.aceptarPorSms(pedido.getId());
            return responder(celular, productorId, "Listo, aceptaste el pedido " + pedido.getCodigo() + ". Prepara: "
                    + AvisosDePedido.resumen(pedido) + ". Entrega en " + pedido.getRegionDestino() + " hasta el "
                    + pedido.getFechaEntregaEstimada().format(AvisosDePedido.FECHA) + ".", pedido.getCodigo(), "ACEPTADO");
        }
        pedidoService.rechazarPorSms(pedido.getId());
        return responder(celular, productorId, "Rechazaste el pedido " + pedido.getCodigo()
                + ". Le devolvemos el dinero al comprador.", pedido.getCodigo(), "RECHAZADO");
    }

    /** "ag 2501" o "ag-2501" → "AG-2501". */
    private static String codigo(String crudo) {
        return crudo == null ? null : "AG-" + crudo.replaceAll("\\D", "");
    }

    private Resultado responder(String celular, Long productorId, String texto, String pedido, String accion) {
        notificaciones.responderSms(celular, productorId, texto);
        return new Resultado(Texto.sinTildes(texto), pedido, accion);
    }
}
