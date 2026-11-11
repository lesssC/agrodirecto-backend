package pe.agrodirecto.confianza.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.confianza.servicio.DisputaService;
import pe.agrodirecto.confianza.servicio.ReputacionService;
import pe.agrodirecto.confianza.servicio.ResenaService;

@RestController
@RequiredArgsConstructor
public class ConfianzaController {

    private final ResenaService resenas;
    private final ReputacionService reputacion;
    private final DisputaService disputas;

    // ---------- Reseñas y reputación ----------

    @PostMapping("/api/pedidos/{id}/resena")
    @ResponseStatus(HttpStatus.CREATED)
    public ResenaResponse calificar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ResenaRequest solicitud) {
        return resenas.crear(jwt, id, solicitud);
    }

    @GetMapping("/api/productores/{id}/resenas")
    public List<ResenaResponse> resenas(@PathVariable Long id) {
        return resenas.delProductor(id);
    }

    @GetMapping("/api/productores/{id}/reputacion")
    public ReputacionResponse reputacion(@PathVariable Long id) {
        return reputacion.de(id);
    }

    /** Ejemplo: {@code /api/reputaciones?productorIds=1,2,3}. */
    @GetMapping("/api/reputaciones")
    public List<ReputacionResponse> reputaciones(@RequestParam List<Long> productorIds) {
        return reputacion.de(productorIds);
    }

    // ---------- Disputas ----------

    @PostMapping("/api/pedidos/{id}/disputa")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputaResponse reportarProblema(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody DisputaRequest solicitud) {
        return disputas.abrir(jwt, id, solicitud);
    }

    @GetMapping("/api/disputas/{id}")
    public DisputaResponse disputa(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return disputas.detalle(jwt, id);
    }

    @PostMapping("/api/disputas/{id}/descargo")
    public DisputaResponse descargo(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody DescargoRequest solicitud) {
        return disputas.descargo(jwt, id, solicitud);
    }
}
