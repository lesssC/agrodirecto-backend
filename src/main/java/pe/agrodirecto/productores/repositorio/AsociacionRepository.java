package pe.agrodirecto.productores.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.productores.dominio.Asociacion;

public interface AsociacionRepository extends JpaRepository<Asociacion, Long> {

    List<Asociacion> findAllByOrderByNombre();
}
