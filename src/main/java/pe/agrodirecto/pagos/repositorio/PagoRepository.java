package pe.agrodirecto.pagos.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.pagos.dominio.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByPedidoId(Long pedidoId);

    List<Pago> findByPedidoIdIn(Collection<Long> pedidoIds);
}
