package pe.agrodirecto.productores.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.productores.servicio.ProductorService;

/** Administración de perfiles de agricultores: el propio productor, su agente de campo o la administración. */
@RestController
@RequestMapping("/api/gestion/productores")
@RequiredArgsConstructor
public class GestionProductorController {

    private final ProductorService servicio;

    @GetMapping
    public List<ProductorGestionResponse> gestionados(@AuthenticationPrincipal Jwt jwt) {
        return servicio.gestionados(jwt);
    }

    @GetMapping("/{id}")
    public ProductorGestionResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.detalleGestion(jwt, id);
    }

    @PostMapping("/mi-perfil")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductorGestionResponse crearPerfilPropio(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductorRequest solicitud) {
        return servicio.crearPerfilPropio(jwt, solicitud);
    }

    /** Registro asistido: el agente da de alta a un agricultor sin computadora (RN-B1). */
    @PostMapping("/asistidos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductorGestionResponse registrarAsistido(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductorRequest solicitud) {
        return servicio.registrarAsistido(jwt, solicitud);
    }

    @PutMapping("/{id}")
    public ProductorGestionResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ProductorRequest solicitud) {
        return servicio.actualizar(jwt, id, solicitud);
    }

    @PostMapping("/{id}/verificar")
    public ProductorGestionResponse verificar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.verificar(jwt, id);
    }

    @PutMapping("/{id}/zonas")
    public ProductorGestionResponse reemplazarZonas(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestBody List<@Valid ZonaRequest> zonas) {
        return servicio.reemplazarZonas(jwt, id, zonas);
    }
}
