package pe.agrodirecto.confianza.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.confianza.dominio.Disputa;
import pe.agrodirecto.confianza.dominio.EstadoDisputa;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    Optional<Disputa> findByPedidoId(Long pedidoId);

    List<Disputa> findAllByOrderByCreadaEnDesc();

    List<Disputa> findByEstadoOrderByCreadaEnAsc(EstadoDisputa estado);

    long countByPedidoProductorIdAndEstado(Long productorId, EstadoDisputa estado);
}
