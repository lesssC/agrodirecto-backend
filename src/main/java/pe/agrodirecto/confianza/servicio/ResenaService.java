package pe.agrodirecto.confianza.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.confianza.dominio.Resena;
import pe.agrodirecto.confianza.repositorio.ResenaRepository;
import pe.agrodirecto.confianza.web.ResenaRequest;
import pe.agrodirecto.confianza.web.ResenaResponse;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.servicio.PedidoService;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

@Service
@RequiredArgsConstructor
public class ResenaService {

    private final ResenaRepository resenas;
    private final PedidoService pedidoService;
    private final UsuarioActualService usuarioActual;
    private final Clock reloj;

    /** RN-20: solo quien compró y recibió puede calificar, y una sola vez por pedido. Evita reseñas falsas. */
    @Transactional
    public ResenaResponse crear(Jwt jwt, Long pedidoId, ResenaRequest solicitud) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        Pedido pedido = pedidoService.buscarComoComprador(comprador, pedidoId);
        if (pedido.getEstado() != EstadoPedido.COMPLETADO) {
            throw new ReglaNegocioException("RN-20", "Solo puedes calificar un pedido que recibiste conforme.");
        }
        if (resenas.existsByPedidoId(pedidoId)) {
            throw new ReglaNegocioException("RN-20", "Ya calificaste el pedido " + pedido.getCodigo() + ".");
        }
        Resena resena = new Resena();
        resena.setPedido(pedido);
        resena.setProductor(pedido.getProductor());
        resena.setAutor(comprador);
        resena.setCalificacion(solicitud.calificacion());
        resena.setComentario(solicitud.comentario());
        resena.setCreadoEn(LocalDateTime.now(reloj));
        return ResenaResponse.de(resenas.save(resena));
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> delProductor(Long productorId) {
        return resenas.findByProductorIdOrderByCreadoEnDesc(productorId).stream().map(ResenaResponse::de).toList();
    }
}
