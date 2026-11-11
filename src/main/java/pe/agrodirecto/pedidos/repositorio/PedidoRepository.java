package pe.agrodirecto.pedidos.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByCompradorIdOrderByCreadoEnDesc(Long compradorId);

    List<Pedido> findByProductorIdInOrderByCreadoEnDesc(Collection<Long> productorIds);

    long countByCompradorIdAndEstado(Long compradorId, EstadoPedido estado);

    long countByCompradorIdAndEstadoIn(Long compradorId, Collection<EstadoPedido> estados);

    /** Entregados hace tiempo sin confirmación ni reclamo: su pago se libera solo (RN-C2). */
    List<Pedido> findByEstadoAndEntregadoEnBefore(EstadoPedido estado, LocalDateTime limite);

    /** Registrados que nunca se pagaron: se cancelan para liberar el stock. */
    List<Pedido> findByEstadoAndCreadoEnBefore(EstadoPedido estado, LocalDateTime limite);
}
