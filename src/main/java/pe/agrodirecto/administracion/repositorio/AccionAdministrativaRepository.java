package pe.agrodirecto.administracion.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.administracion.dominio.AccionAdministrativa;

public interface AccionAdministrativaRepository extends JpaRepository<AccionAdministrativa, Long> {

    List<AccionAdministrativa> findTop100ByOrderByCreadoEnDescIdDesc();

    List<AccionAdministrativa> findByObjetivoAndObjetivoIdOrderByCreadoEnDescIdDesc(String objetivo, Long objetivoId);
}
