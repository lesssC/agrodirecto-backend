package pe.agrodirecto.pedidos.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.catalogo.repositorio.FormatoVentaRepository;
import pe.agrodirecto.compartido.Regiones;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.pedidos.dominio.CanalOperacion;
import pe.agrodirecto.pedidos.dominio.DetallePedido;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.repositorio.PedidoRepository;
import pe.agrodirecto.pedidos.servicio.CalculadoraPedido.Linea;
import pe.agrodirecto.pedidos.servicio.CalculadoraPedido.Resultado;
import pe.agrodirecto.pedidos.servicio.PedidoMapper.Vista;
import pe.agrodirecto.pedidos.web.CotizacionResponse;
import pe.agrodirecto.pedidos.web.DesgloseResponse;
import pe.agrodirecto.pedidos.web.ItemPedidoRequest;
import pe.agrodirecto.pedidos.web.PedidoRequest;
import pe.agrodirecto.pedidos.web.PedidoResponse;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.dominio.ZonaCobertura;
import pe.agrodirecto.productores.servicio.ProductorService;
import pe.agrodirecto.productores.web.ProductorGestionResponse;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

@Service
@RequiredArgsConstructor
public class PedidoService {

    /** Días que el agricultor necesita para cosechar y empacar antes del despacho. */
    private static final int DIAS_PREPARACION = 1;

    private final PedidoRepository pedidos;
    private final FormatoVentaRepository formatos;
    private final CalculadoraPedido calculadora;
    private final PedidoMapper mapper;
    private final UsuarioActualService usuarioActual;
    private final ProductorService productorService;
    private final AgroDirectoProperties propiedades;
    private final Clock reloj;

    // ---------- Comprador ----------

    /** Vista previa con las mismas validaciones del pedido real, para mostrar el desglose antes de confirmar. */
    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(Jwt jwt, PedidoRequest solicitud) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        Preparacion prep = preparar(comprador, solicitud);
        Resultado r = prep.resultado();
        return new CotizacionResponse(prep.productor().getId(), prep.productor().getNombreComercial(),
                prep.productor().emiteFactura(), solicitud.modalidadEntrega(), prep.regionDestino(),
                prep.diasTransito(), prep.fechaEntrega(),
                r.lineas().stream().map(PedidoMapper::linea).toList(),
                DesgloseResponse.de(r.subtotal(), r.descuento(), r.costoFlete(), r.comision(), r.total(),
                        r.montoProductor(), prep.productor().getNombreComercial()));
    }

    @Transactional
    public PedidoResponse crear(Jwt jwt, PedidoRequest solicitud) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        Preparacion prep = preparar(comprador, solicitud);
        Resultado r = prep.resultado();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Pedido pedido = new Pedido();
        pedido.setComprador(comprador);
        pedido.setProductor(prep.productor());
        pedido.setModalidadEntrega(solicitud.modalidadEntrega());
        pedido.setRegionDestino(prep.regionDestino());
        pedido.setDireccionEntrega(solicitud.direccionEntrega().trim());
        pedido.setNota(solicitud.nota());
        pedido.setSubtotal(r.subtotal());
        pedido.setDescuento(r.descuento());
        pedido.setCostoFlete(r.costoFlete());
        pedido.setComision(r.comision());
        pedido.setTotal(r.total());
        pedido.setMontoProductor(r.montoProductor());
        pedido.setFechaEntregaEstimada(prep.fechaEntrega());
        for (CalculadoraPedido.LineaCalculada linea : r.lineas()) {
            linea.formato().reservar(linea.cantidad()); // RN-03: el stock queda separado para este pedido
            DetallePedido detalle = new DetallePedido();
            detalle.setFormato(linea.formato());
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(linea.precioUnitario());
            detalle.setPorcentajeDescuento(linea.porcentajeDescuento());
            detalle.setSubtotal(linea.neto());
            pedido.agregarDetalle(detalle);
        }
        pedido.registrar(EstadoPedido.PENDIENTE, comprador, CanalOperacion.WEB, ahora);
        pedidos.saveAndFlush(pedido);
        pedido.asignarCodigo();
        return mapper.aRespuesta(pedido, Vista.COMPRADOR);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> misPedidos(Jwt jwt, EstadoPedido estado) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        return pedidos.findByCompradorIdOrderByCreadoEnDesc(comprador.getId()).stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .map(p -> mapper.aRespuesta(p, Vista.COMPRADOR)).toList();
    }

    /** Solo antes de que el agricultor acepte: después ya pudo haber cosechado (protege al agricultor). */
    @Transactional
    public PedidoResponse cancelar(Jwt jwt, Long id, String motivo) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        Pedido pedido = delComprador(comprador, id);
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ReglaNegocioException("RN-05",
                    "El agricultor ya aceptó el pedido; para cualquier problema usa el reclamo tras la entrega.");
        }
        pedido.cambiarEstado(EstadoPedido.CANCELADO, comprador, CanalOperacion.WEB, motivo, LocalDateTime.now(reloj));
        reponerStock(pedido);
        return mapper.aRespuesta(pedido, Vista.COMPRADOR);
    }

    @Transactional
    public PedidoResponse confirmarRecepcion(Jwt jwt, Long id) {
        Usuario comprador = usuarioActual.exigirRol(jwt, Rol.COMPRADOR);
        Pedido pedido = delComprador(comprador, id);
        pedido.cambiarEstado(EstadoPedido.COMPLETADO, comprador, CanalOperacion.WEB, "Recepción conforme",
                LocalDateTime.now(reloj));
        return mapper.aRespuesta(pedido, Vista.COMPRADOR);
    }

    // ---------- Productor, agente o administración ----------

    @Transactional(readOnly = true)
    public List<PedidoResponse> recibidos(Jwt jwt, Long productorId, EstadoPedido estado) {
        List<Long> ids = productorId != null
                ? List.of(productorService.buscarGestionable(jwt, productorId).getId())
                : productorService.gestionados(jwt).stream().map(ProductorGestionResponse::id).toList();
        return pedidos.findByProductorIdInOrderByCreadoEnDesc(ids).stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .map(p -> mapper.aRespuesta(p, Vista.GESTOR)).toList();
    }

    @Transactional
    public PedidoResponse aceptar(Jwt jwt, Long id) {
        Usuario actual = usuarioActual.obtener(jwt);
        Pedido pedido = gestionable(actual, id);
        pedido.cambiarEstado(EstadoPedido.ACEPTADO, actual, CanalOperacion.WEB, null, LocalDateTime.now(reloj));
        return mapper.aRespuesta(pedido, Vista.GESTOR);
    }

    @Transactional
    public PedidoResponse rechazar(Jwt jwt, Long id, String motivo) {
        Usuario actual = usuarioActual.obtener(jwt);
        Pedido pedido = gestionable(actual, id);
        pedido.cambiarEstado(EstadoPedido.RECHAZADO, actual, CanalOperacion.WEB, motivo, LocalDateTime.now(reloj));
        reponerStock(pedido);
        return mapper.aRespuesta(pedido, Vista.GESTOR);
    }

    /** Avances del agricultor tras aceptar: en preparación, listo y entregado. */
    @Transactional
    public PedidoResponse avanzar(Jwt jwt, Long id, EstadoPedido estado, String comentario) {
        if (!Set.of(EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_PARA_ENTREGA, EstadoPedido.ENTREGADO)
                .contains(estado)) {
            throw new ReglaNegocioException("RN-05", "Usa la acción específica para pasar el pedido a " + estado + ".");
        }
        Usuario actual = usuarioActual.obtener(jwt);
        Pedido pedido = gestionable(actual, id);
        pedido.cambiarEstado(estado, actual, CanalOperacion.WEB, comentario, LocalDateTime.now(reloj));
        return mapper.aRespuesta(pedido, Vista.GESTOR);
    }

    // ---------- Ambos ----------

    @Transactional(readOnly = true)
    public PedidoResponse detalle(Jwt jwt, Long id) {
        Usuario actual = usuarioActual.obtener(jwt);
        Pedido pedido = buscar(id);
        if (pedido.esDelComprador(actual)) {
            return mapper.aRespuesta(pedido, Vista.COMPRADOR);
        }
        if (pedido.getProductor().esGestionadoPor(actual)) {
            return mapper.aRespuesta(pedido, Vista.GESTOR);
        }
        throw new AccesoDenegadoException("Este pedido no es tuyo.");
    }

    // ---------- Reglas de armado del pedido ----------

    private record Preparacion(Productor productor, String regionDestino, int diasTransito, LocalDate fechaEntrega,
            Resultado resultado) {
    }

    private Preparacion preparar(Usuario comprador, PedidoRequest solicitud) {
        List<Linea> lineas = cargarLineas(solicitud.items());
        Productor productor = unicoProductor(lineas);
        String region = Regiones.normalizar(solicitud.regionDestino()).orElseThrow(() -> new ReglaNegocioException(
                "RN-09", "La región \"" + solicitud.regionDestino() + "\" no existe."));

        // RN-09 / RN-12: con recojo en chacra no hay flete ni restricción de zona.
        int dias = 0;
        BigDecimal flete = BigDecimal.ZERO;
        ZonaCobertura zona = null;
        if (solicitud.modalidadEntrega().requiereTransporte()) {
            zona = productor.zonaPara(region).orElseThrow(() -> new ReglaNegocioException("RN-09",
                    productor.getNombreComercial() + " no entrega en " + region
                            + ". Puedes elegir recojo en chacra o buscar un productor que atienda tu región."));
            dias = zona.getDiasTransito();
            flete = zona.getCostoFlete();
            validarVidaUtil(lineas, zona);
        }

        Resultado resultado = calculadora.calcular(lineas, flete);
        if (zona != null && resultado.neto().compareTo(zona.getPedidoMinimo()) < 0) {
            throw new ReglaNegocioException("RN-11", "El pedido mínimo de " + productor.getNombreComercial()
                    + " para " + region + " es S/ " + zona.getPedidoMinimo() + " (llevas S/ " + resultado.neto()
                    + "). Así el viaje vale la pena para el agricultor.");
        }
        validarCuentaNueva(comprador, resultado.total());
        LocalDate entrega = LocalDate.now(reloj).plusDays(DIAS_PREPARACION + dias);
        return new Preparacion(productor, region, dias, entrega, resultado);
    }

    private List<Linea> cargarLineas(List<ItemPedidoRequest> items) {
        Set<Long> ids = new HashSet<>();
        for (ItemPedidoRequest item : items) {
            if (!ids.add(item.formatoId())) {
                throw new ReglaNegocioException("RN-03", "Una misma presentación aparece dos veces en el carrito.");
            }
        }
        Map<Long, FormatoVenta> encontrados = formatos.findAllById(ids).stream()
                .collect(Collectors.toMap(FormatoVenta::getId, Function.identity()));
        return items.stream().map(item -> {
            FormatoVenta formato = encontrados.get(item.formatoId());
            if (formato == null) {
                throw new NoEncontradoException("Presentación", item.formatoId());
            }
            Producto producto = formato.getProducto();
            if (!producto.visibleEnCatalogo() || !formato.isActivo()) {
                throw new ReglaNegocioException("RN-03", producto.getNombre() + " (" + formato.getNombre()
                        + ") ya no está disponible.");
            }
            if (item.cantidad() > formato.getStock()) {
                throw new ReglaNegocioException("RN-03", "Solo quedan " + formato.getStock() + " de «"
                        + formato.getNombre() + "» de " + producto.getNombre() + ".");
            }
            return new Linea(formato, item.cantidad());
        }).toList();
    }

    /** RN-01: un pedido, un productor. Simplifica la entrega y la responsabilidad. */
    private static Productor unicoProductor(List<Linea> lineas) {
        Productor primero = lineas.getFirst().formato().getProducto().getProductor();
        boolean mezcla = lineas.stream()
                .anyMatch(l -> !l.formato().getProducto().getProductor().getId().equals(primero.getId()));
        if (mezcla) {
            throw new ReglaNegocioException("RN-01",
                    "Cada pedido agrupa productos de un solo productor. Separa tu carrito en un pedido por productor.");
        }
        return primero;
    }

    /** RN-10: no se vende lo que llegaría malogrado. */
    private static void validarVidaUtil(List<Linea> lineas, ZonaCobertura zona) {
        for (Linea linea : lineas) {
            Producto producto = linea.formato().getProducto();
            Integer vidaUtil = producto.getVidaUtilDias();
            if (vidaUtil != null && zona.getDiasTransito() >= vidaUtil) {
                throw new ReglaNegocioException("RN-10", producto.getNombre() + " se conserva " + vidaUtil
                        + " días y el viaje a " + zona.getRegion() + " toma " + zona.getDiasTransito()
                        + "; llegaría en mal estado. Elige otra presentación, otro producto o recojo en chacra.");
            }
        }
    }

    /**
     * RN-18: una cuenta sin verificar y sin historial tiene límites, para que nadie haga pedidos grandes
     * que luego no recoge y el agricultor pierda su cosecha.
     */
    private void validarCuentaNueva(Usuario comprador, BigDecimal total) {
        AgroDirectoProperties.CuentaNueva limites = propiedades.cuentaNueva();
        if (comprador.isVerificado()
                || pedidos.countByCompradorIdAndEstado(comprador.getId(), EstadoPedido.COMPLETADO)
                        >= limites.pedidosParaConfianza()) {
            return;
        }
        if (total.compareTo(limites.montoMaximo()) > 0) {
            throw new ReglaNegocioException("RN-18", "Como cuenta nueva, cada pedido puede ser de hasta S/ "
                    + limites.montoMaximo() + " hasta completar " + limites.pedidosParaConfianza()
                    + " compras o verificar tu identidad.");
        }
        if (pedidos.countByCompradorIdAndEstadoIn(comprador.getId(), EstadoPedido.abiertos())
                >= limites.pedidosAbiertosMaximo()) {
            throw new ReglaNegocioException("RN-18", "Como cuenta nueva puedes tener hasta "
                    + limites.pedidosAbiertosMaximo() + " pedidos en curso a la vez.");
        }
    }

    // ---------- Apoyo ----------

    private void reponerStock(Pedido pedido) {
        pedido.getDetalles().forEach(d -> d.getFormato().reponer(d.getCantidad()));
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NoEncontradoException("Pedido", id));
    }

    private Pedido delComprador(Usuario comprador, Long id) {
        Pedido pedido = buscar(id);
        if (!pedido.esDelComprador(comprador)) {
            throw new AccesoDenegadoException("Este pedido no es tuyo.");
        }
        return pedido;
    }

    private Pedido gestionable(Usuario actual, Long id) {
        Pedido pedido = buscar(id);
        if (!pedido.getProductor().esGestionadoPor(actual)) {
            throw new AccesoDenegadoException("No gestionas al productor de este pedido.");
        }
        return pedido;
    }
}
