package pe.agrodirecto.catalogo.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.servicio.GestionProductoService;

@RestController
@RequestMapping("/api/gestion/productos")
@RequiredArgsConstructor
public class GestionProductoController {

    private final GestionProductoService servicio;

    @GetMapping
    public List<ProductoGestionResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long productorId) {
        return servicio.listar(jwt, productorId);
    }

    @GetMapping("/{id}")
    public ProductoGestionResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.detalle(jwt, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoGestionResponse crear(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductoRequest solicitud) {
        return servicio.crear(jwt, solicitud);
    }

    @PutMapping("/{id}")
    public ProductoGestionResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ProductoRequest solicitud) {
        return servicio.actualizar(jwt, id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public ProductoGestionResponse cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody EstadoProductoRequest solicitud) {
        return servicio.cambiarEstado(jwt, id, solicitud.estado());
    }
}
