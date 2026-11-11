package pe.agrodirecto.confianza.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.confianza.dominio.Disputa;
import pe.agrodirecto.confianza.dominio.EstadoDisputa;
import pe.agrodirecto.confianza.repositorio.DisputaRepository;
import pe.agrodirecto.confianza.web.DescargoRequest;
import pe.agrodirecto.confianza.web.DisputaRequest;
import pe.agrodirecto.confianza.web.DisputaResponse;
import pe.agrodirecto.confianza.web.ResolucionRequest;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.servicio.PedidoService;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

/**
 * RN-22: disputas. El comprador reporta con evidencia, el agricultor presenta su descargo y la administración
 * decide. Mientras tanto el pago queda congelado: no se libera solo ni se devuelve sin revisión.
 */
@Service
@RequiredArgsConstructor
public class DisputaService {

    private final DisputaRepository disputas;
    private final PedidoService pedidoService;
    private final UsuarioActualService usuarioActual;
    private final Clock reloj;

    @Transactional
    public DisputaResponse abrir(Jwt jwt, Long pedidoId, DisputaRequest solicitud) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        if (solicitud.motivo().requiereEvidencia()
                && (solicitud.evidenciaUrl() == null || solicitud.evidenciaUrl().isBlank())) {
            throw new ReglaNegocioException("RN-15",
                    "Para un reclamo por " + solicitud.motivo() + " adjunta una foto de lo que recibiste.");
        }
        Pedido pedido = pedidoService.abrirDisputa(pedidoId, comprador, "Reclamo: " + solicitud.motivo());
        Disputa disputa = new Disputa();
        disputa.setPedido(pedido);
        disputa.setAbiertaPor(comprador);
        disputa.setMotivo(solicitud.motivo());
        disputa.setDescripcion(solicitud.descripcion().trim());
        disputa.setEvidenciaUrl(solicitud.evidenciaUrl());
        disputa.setCreadaEn(LocalDateTime.now(reloj));
        return DisputaResponse.de(disputas.save(disputa));
    }

    /** El agricultor, o su agente en su nombre, cuenta su versión antes de que se decida. */
    @Transactional
    public DisputaResponse descargo(Jwt jwt, Long id, DescargoRequest solicitud) {
        Usuario actual = usuarioActual.obtener(jwt);
        Disputa disputa = buscar(id);
        if (!disputa.getPedido().getProductor().esGestionadoPor(actual)) {
            throw new AccesoDenegadoException("Solo el agricultor o su agente pueden presentar el descargo.");
        }
        disputa.registrarDescargo(solicitud.descargo().trim(), solicitud.evidenciaUrl(), LocalDateTime.now(reloj));
        return DisputaResponse.de(disputa);
    }

    @Transactional(readOnly = true)
    public DisputaResponse detalle(Jwt jwt, Long id) {
        Usuario actual = usuarioActual.obtener(jwt);
        Disputa disputa = buscar(id);
        Pedido pedido = disputa.getPedido();
        if (!pedido.esDelComprador(actual) && !pedido.getProductor().esGestionadoPor(actual)) {
            throw new AccesoDenegadoException("No participas en esta disputa.");
        }
        return DisputaResponse.de(disputa);
    }

    @Transactional(readOnly = true)
    public List<DisputaResponse> listar(Jwt jwt, EstadoDisputa estado) {
        usuarioActual.exigirRol(jwt, Rol.ADMINISTRADOR);
        List<Disputa> lista = estado == null ? disputas.findAllByOrderByCreadaEnDesc()
                : disputas.findByEstadoOrderByCreadaEnAsc(estado);
        return lista.stream().map(DisputaResponse::de).toList();
    }

    @Transactional
    public DisputaResponse resolver(Jwt jwt, Long id, ResolucionRequest solicitud) {
        Usuario administrador = usuarioActual.exigirRol(jwt, Rol.ADMINISTRADOR);
        Disputa disputa = buscar(id);
        boolean aFavorDelComprador = solicitud.aFavorDe() == ResolucionRequest.ParteFavorecida.COMPRADOR;
        disputa.resolver(aFavorDelComprador ? EstadoDisputa.A_FAVOR_COMPRADOR : EstadoDisputa.A_FAVOR_PRODUCTOR,
                solicitud.resolucion().trim(), administrador, LocalDateTime.now(reloj));
        pedidoService.resolverDisputa(disputa.getPedido().getId(), administrador, aFavorDelComprador,
                "Disputa resuelta: " + solicitud.resolucion().trim());
        return DisputaResponse.de(disputa);
    }

    private Disputa buscar(Long id) {
        return disputas.findById(id).orElseThrow(() -> new NoEncontradoException("Disputa", id));
    }
}
