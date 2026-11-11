package pe.agrodirecto.productores.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;

public interface ProductorRepository extends JpaRepository<Productor, Long> {

    boolean existsByDni(String dni);

    boolean existsByUsuarioId(Long usuarioId);

    Optional<Productor> findByUsuarioId(Long usuarioId);

    List<Productor> findByAgenteIdOrderByNombreComercial(Long agenteId);

    List<Productor> findAllByOrderByNombreComercial();

    /** Directorio público: solo perfiles activos, con filtros opcionales por región de origen o región atendida. */
    @Query("""
            SELECT DISTINCT p FROM Productor p LEFT JOIN p.zonas z
            WHERE p.estado = :estado
              AND (:region IS NULL OR p.region = :region)
              AND (:atiende IS NULL OR z.region = :atiende)
            ORDER BY p.nombreComercial
            """)
    List<Productor> buscarDirectorio(@Param("estado") EstadoCuenta estado, @Param("region") String region,
            @Param("atiende") String atiende);
}
