package pe.agrodirecto.tiemporeal.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.productores.repositorio.ProductorRepository;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;

/** Mismas reglas que la API REST: solo quien gestiona al productor escucha sus pedidos. */
@Service
@RequiredArgsConstructor
public class PermisosTiempoReal {

    private final UsuarioRepository usuarios;
    private final ProductorRepository productores;

    @Transactional(readOnly = true)
    public boolean puedeEscucharProductor(Long usuarioId, Long productorId) {
        Usuario usuario = usuarios.findById(usuarioId).filter(Usuario::estaActivo).orElse(null);
        return usuario != null && productores.findById(productorId)
                .map(productor -> productor.esGestionadoPor(usuario))
                .orElse(false);
    }
}
