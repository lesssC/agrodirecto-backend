package pe.agrodirecto.catalogo.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.agrodirecto.catalogo.dominio.EstadoProducto;
import pe.agrodirecto.catalogo.dominio.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Productos publicados de productores activos. Los filtros nulos no se aplican.
     * El texto libre y la categoría se filtran después en memoria para ignorar tildes de forma uniforme.
     */
    @Query("""
            SELECT DISTINCT p FROM Producto p
              JOIN FETCH p.productor pr
              JOIN FETCH p.categoria c
              LEFT JOIN pr.zonas z
            WHERE p.estado = :activo
              AND pr.estado = pe.agrodirecto.usuarios.dominio.EstadoCuenta.ACTIVO
              AND (:productorId IS NULL OR pr.id = :productorId)
              AND (:region IS NULL OR pr.region = :region)
              AND (:atiende IS NULL OR z.region = :atiende)
              AND (:organico IS NULL OR p.organico = :organico)
            ORDER BY p.id
            """)
    List<Producto> buscarPublicados(@Param("activo") EstadoProducto activo, @Param("productorId") Long productorId,
            @Param("region") String region, @Param("atiende") String atiende, @Param("organico") Boolean organico);

    List<Producto> findByProductorIdInOrderByActualizadoEnDesc(Collection<Long> productorIds);

    List<Producto> findByIdIn(Collection<Long> ids);
}
