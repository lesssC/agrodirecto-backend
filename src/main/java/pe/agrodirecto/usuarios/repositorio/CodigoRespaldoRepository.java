package pe.agrodirecto.usuarios.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.usuarios.dominio.CodigoRespaldo;

public interface CodigoRespaldoRepository extends JpaRepository<CodigoRespaldo, Long> {

    Optional<CodigoRespaldo> findByUsuarioIdAndHashAndUsadoFalse(Long usuarioId, String hash);

    List<CodigoRespaldo> findByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndUsadoFalse(Long usuarioId);
}
