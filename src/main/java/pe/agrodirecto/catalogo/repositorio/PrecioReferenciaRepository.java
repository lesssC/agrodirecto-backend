package pe.agrodirecto.catalogo.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.catalogo.dominio.PrecioReferencia;

public interface PrecioReferenciaRepository extends JpaRepository<PrecioReferencia, Long> {

    List<PrecioReferencia> findAllByOrderByNombre();

    boolean existsByClave(String clave);
}
