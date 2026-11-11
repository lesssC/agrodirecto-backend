package pe.agrodirecto.pagos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.agrodirecto.compartido.error.ReglaNegocioException;

/**
 * Pago con retención (escrow). El comprador paga primero, el dinero queda retenido y solo se libera al
 * agricultor cuando la entrega se confirma. Protege a ambos: el agricultor no prepara un pedido que nadie pagará
 * y el comprador no pierde su dinero si no recibe lo que compró.
 *
 * <p>El módulo solo conoce el id del pedido, así {@code pagos} no depende de {@code pedidos}.
 */
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    /** Total que paga el comprador. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MetodoPago metodo;

    @Column(length = 60)
    private String referencia;

    @Column(name = "monto_productor", precision = 12, scale = 2)
    private BigDecimal montoProductor;

    @Column(precision = 12, scale = 2)
    private BigDecimal comision;

    @Column(name = "destino_metodo", length = 20)
    private String destinoMetodo;

    @Column(name = "destino_numero", length = 30)
    private String destinoNumero;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "pagado_en")
    private LocalDateTime pagadoEn;

    @Column(name = "liberado_en")
    private LocalDateTime liberadoEn;

    @Column(name = "devuelto_en")
    private LocalDateTime devueltoEn;

    public void retener(MetodoPago metodoPago, String referenciaOperacion, LocalDateTime ahora) {
        exigir(EstadoPago.PENDIENTE, "Este pedido ya fue pagado.");
        estado = EstadoPago.RETENIDO;
        metodo = metodoPago;
        referencia = referenciaOperacion;
        pagadoEn = ahora;
    }

    /** RN-C2: transfiere al agricultor su parte y deja constancia de a qué cuenta se envió. */
    public void liberar(Liquidacion liquidacion, LocalDateTime ahora) {
        exigir(EstadoPago.RETENIDO, "Solo se libera un pago retenido.");
        estado = EstadoPago.LIBERADO;
        montoProductor = liquidacion.montoProductor();
        comision = liquidacion.comision();
        destinoMetodo = liquidacion.metodo();
        destinoNumero = liquidacion.numero();
        liberadoEn = ahora;
    }

    /** Devuelve el dinero si se pagó; si no se llegó a pagar, solo anula. */
    public void revertir(LocalDateTime ahora) {
        if (estado == EstadoPago.RETENIDO) {
            estado = EstadoPago.DEVUELTO;
            devueltoEn = ahora;
        } else if (estado == EstadoPago.PENDIENTE) {
            estado = EstadoPago.ANULADO;
        } else {
            throw new ReglaNegocioException("RN-C1", "El pago ya está " + estado + " y no se puede revertir.");
        }
    }

    private void exigir(EstadoPago esperado, String mensaje) {
        if (estado != esperado) {
            throw new ReglaNegocioException("RN-C1", mensaje);
        }
    }

    /** A quién y cuánto se paga al liberar. */
    public record Liquidacion(BigDecimal montoProductor, BigDecimal comision, String metodo, String numero) {
    }
}
