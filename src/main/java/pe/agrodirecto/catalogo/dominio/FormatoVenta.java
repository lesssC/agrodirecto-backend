package pe.agrodirecto.catalogo.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.agrodirecto.compartido.error.ReglaNegocioException;

/**
 * Presentación de venta de un producto (RN-02), con su propio precio y stock.
 * {@code version} activa el bloqueo optimista: si dos compradores reservan el mismo stock a la vez,
 * solo uno lo consigue y el otro recibe un aviso para reintentar.
 */
@Entity
@Table(name = "formato_venta")
@Getter
@Setter
@NoArgsConstructor
public class FormatoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFormato tipo;

    @Column(name = "peso_kg", precision = 10, scale = 3)
    private BigDecimal pesoKg;

    /** RN-A1: precio fijo que define el agricultor. El comprador no puede contraofertar. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private int stock;

    /** RN-A1: el único descuento posible es por volumen, y lo decide el agricultor. */
    @Column(name = "cantidad_min_descuento")
    private Integer cantidadMinDescuento;

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    private long version;

    public boolean disponible() {
        return activo && stock > 0;
    }

    public Optional<BigDecimal> precioPorKg() {
        if (pesoKg == null || pesoKg.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(precio.divide(pesoKg, 2, RoundingMode.HALF_UP));
    }

    public BigDecimal porcentajeDescuentoPara(int cantidad) {
        if (cantidadMinDescuento != null && porcentajeDescuento != null && cantidad >= cantidadMinDescuento) {
            return porcentajeDescuento;
        }
        return BigDecimal.ZERO;
    }

    /** RN-03: descuenta stock al registrar un pedido. */
    public void reservar(int cantidad) {
        if (!activo) {
            throw new ReglaNegocioException("RN-03", "La presentación «" + nombre + "» ya no está a la venta.");
        }
        if (cantidad > stock) {
            throw new ReglaNegocioException("RN-03", "Solo quedan " + stock + " unidades de «" + nombre + "» de "
                    + producto.getNombre() + ".");
        }
        stock -= cantidad;
    }

    /** Devuelve stock cuando un pedido se cancela o se rechaza. */
    public void reponer(int cantidad) {
        stock += cantidad;
    }
}
