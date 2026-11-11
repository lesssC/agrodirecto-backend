package pe.agrodirecto.confianza.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.agrodirecto.confianza.dominio.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    boolean existsByPedidoId(Long pedidoId);

    List<Resena> findByProductorIdOrderByCreadoEnDesc(Long productorId);

    @Query("SELECT AVG(r.calificacion) FROM Resena r WHERE r.productor.id = :productorId")
    Double promedio(@Param("productorId") Long productorId);

    long countByProductorId(Long productorId);
}
