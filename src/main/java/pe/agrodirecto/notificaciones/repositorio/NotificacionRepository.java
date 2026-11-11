package pe.agrodirecto.notificaciones.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.notificaciones.dominio.CanalNotificacion;
import pe.agrodirecto.notificaciones.dominio.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndCanalOrderByCreadaEnDescIdDesc(Long usuarioId, CanalNotificacion canal);

    List<Notificacion> findByProductorIdAndCanalInOrderByCreadaEnDescIdDesc(Long productorId,
            List<CanalNotificacion> canales);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndCanalAndLeidaFalse(Long usuarioId, CanalNotificacion canal);
}
