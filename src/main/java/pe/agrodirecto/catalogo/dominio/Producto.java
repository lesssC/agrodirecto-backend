package pe.agrodirecto.catalogo.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import pe.agrodirecto.productores.dominio.Productor;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productor_id")
    private Productor productor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "precio_referencia_id")
    private PrecioReferencia precioReferencia;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private boolean organico;

    @Column(name = "fecha_cosecha")
    private LocalDate fechaCosecha;

    /** Días que el producto aguanta en buen estado; se compara con el tiempo de tránsito (RN-10). */
    @Column(name = "vida_util_dias")
    private Integer vidaUtilDias;

    /** Calibre o grado declarado; base para resolver reclamos de calidad (RN-15). */
    @Column(length = 60)
    private String calibre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProducto estado = EstadoProducto.BORRADOR;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
    @OrderBy("precio ASC")
    private List<FormatoVenta> formatos = new ArrayList<>();

    public boolean visibleEnCatalogo() {
        return estado == EstadoProducto.ACTIVO && productor.estaActivo();
    }

    public List<FormatoVenta> formatosActivos() {
        return formatos.stream().filter(FormatoVenta::isActivo).toList();
    }

    /** Presentación más barata a la venta: es el "desde S/ X" de la tarjeta del catálogo. */
    public Optional<FormatoVenta> formatoDesde() {
        return formatosActivos().stream().min(Comparator.comparing(FormatoVenta::getPrecio));
    }

    public Optional<BigDecimal> precioDesde() {
        return formatoDesde().map(FormatoVenta::getPrecio);
    }

    public boolean disponible() {
        return formatosActivos().stream().anyMatch(FormatoVenta::disponible);
    }

    public void agregarFormato(FormatoVenta formato) {
        formato.setProducto(this);
        formatos.add(formato);
    }
}
