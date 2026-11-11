package pe.agrodirecto.pedidos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.usuarios.dominio.Usuario;

/**
 * Pedido de un comprador a un único productor (RN-01). Los montos se calculan en el servidor y se congelan
 * al registrarlo: el comprador nunca envía precios (RN-A1).
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    /** Los códigos visibles continúan la numeración del prototipo (AG-2408, AG-2412...). */
    public static final long BASE_CODIGO = 2500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprador_id")
    private Usuario comprador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productor_id")
    private Productor productor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega", nullable = false, length = 30)
    private ModalidadEntrega modalidadEntrega;

    @Column(name = "region_destino", nullable = false, length = 60)
    private String regionDestino;

    @Column(name = "direccion_entrega", nullable = false, length = 250)
    private String direccionEntrega;

    @Column(length = 500)
    private String nota;

    /** Suma de precio × cantidad, antes del descuento por volumen. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "costo_flete", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoFlete;

    /** RN-A4: servicio de la plataforma, pagado por el comprador. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comision;

    /** Lo que paga el comprador. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** Lo que recibe el agricultor: su precio íntegro más el flete. */
    @Column(name = "monto_productor", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoProductor;

    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    @Column(name = "entregado_en")
    private LocalDateTime entregadoEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("creadoEn ASC, id ASC")
    private List<HistorialPedido> historial = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    /** Registra el estado inicial en el historial. */
    public void registrar(EstadoPedido inicial, Usuario usuario, CanalOperacion canal, LocalDateTime ahora) {
        estado = inicial;
        creadoEn = ahora;
        actualizadoEn = ahora;
        anotar(null, inicial, usuario, canal, null, ahora);
    }

    /** RN-05: aplica una transición válida y la deja en el historial; si no es válida, la rechaza. */
    public void cambiarEstado(EstadoPedido nuevo, Usuario usuario, CanalOperacion canal, String comentario,
            LocalDateTime ahora) {
        if (!estado.puedePasarA(nuevo)) {
            throw new ReglaNegocioException("RN-05",
                    "El pedido " + codigo + " está " + estado + " y no puede pasar a " + nuevo + ".");
        }
        EstadoPedido anterior = estado;
        estado = nuevo;
        actualizadoEn = ahora;
        if (nuevo == EstadoPedido.ENTREGADO) {
            entregadoEn = ahora;
        }
        anotar(anterior, nuevo, usuario, canal, comentario, ahora);
    }

    public boolean esDelComprador(Usuario usuario) {
        return Objects.equals(comprador.getId(), usuario.getId());
    }

    /** Se llama después de guardar, cuando ya existe el id. */
    public void asignarCodigo() {
        codigo = "AG-" + (BASE_CODIGO + id);
    }

    private void anotar(EstadoPedido anterior, EstadoPedido nuevo, Usuario usuario, CanalOperacion canal,
            String comentario, LocalDateTime ahora) {
        HistorialPedido entrada = new HistorialPedido();
        entrada.setPedido(this);
        entrada.setEstadoAnterior(anterior);
        entrada.setEstadoNuevo(nuevo);
        entrada.setUsuario(usuario);
        entrada.setCanal(canal);
        entrada.setComentario(comentario);
        entrada.setCreadoEn(ahora);
        historial.add(entrada);
    }
}
