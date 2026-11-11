package pe.agrodirecto.catalogo.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.servicio.FavoritoService;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService servicio;

    @GetMapping
    public List<ProductoResumenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(jwt);
    }

    /** Idempotente: guardar dos veces el mismo producto no genera duplicados. */
    @PutMapping("/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void agregar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productoId) {
        servicio.agregar(jwt, productoId);
    }

    @DeleteMapping("/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productoId) {
        servicio.quitar(jwt, productoId);
    }
}
