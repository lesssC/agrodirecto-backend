package pe.agrodirecto.catalogo.servicio;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.Favorito;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.catalogo.repositorio.FavoritoRepository;
import pe.agrodirecto.catalogo.repositorio.ProductoRepository;
import pe.agrodirecto.catalogo.web.ProductoResumenResponse;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

/** Productos guardados por el usuario; reemplaza a los favoritos que el prototipo guardaba en localStorage. */
@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritos;
    private final ProductoRepository productos;
    private final UsuarioActualService usuarioActual;

    /** Solo devuelve los que siguen visibles en el catálogo. */
    @Transactional(readOnly = true)
    public List<ProductoResumenResponse> listar(Jwt jwt) {
        Long usuarioId = usuarioActual.obtener(jwt).getId();
        List<Long> ids = favoritos.findByIdUsuarioId(usuarioId).stream().map(f -> f.getId().getProductoId()).toList();
        return productos.findByIdIn(ids).stream().filter(Producto::visibleEnCatalogo)
                .map(ProductoResumenResponse::de).toList();
    }

    @Transactional
    public void agregar(Jwt jwt, Long productoId) {
        Long usuarioId = usuarioActual.obtener(jwt).getId();
        productos.findById(productoId).filter(Producto::visibleEnCatalogo)
                .orElseThrow(() -> new NoEncontradoException("Producto", productoId));
        Favorito.Clave clave = new Favorito.Clave(usuarioId, productoId);
        if (!favoritos.existsById(clave)) {
            favoritos.save(new Favorito(clave));
        }
    }

    @Transactional
    public void quitar(Jwt jwt, Long productoId) {
        favoritos.deleteById(new Favorito.Clave(usuarioActual.obtener(jwt).getId(), productoId));
    }
}
