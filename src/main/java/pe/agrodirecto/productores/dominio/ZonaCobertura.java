package pe.agrodirecto.productores.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Región que el productor puede abastecer, con su tiempo de tránsito, pedido mínimo y flete (RN-09, RN-10, RN-11). */
@Entity
@Table(name = "zona_cobertura")
@Getter
@Setter
@NoArgsConstructor
public class ZonaCobertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productor_id")
    private Productor productor;

    @Column(nullable = false, length = 60)
    private String region;

    @Column(name = "dias_transito", nullable = false)
    private int diasTransito;

    @Column(name = "pedido_minimo", nullable = false, precision = 10, scale = 2)
    private BigDecimal pedidoMinimo;

    @Column(name = "costo_flete", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoFlete;
}
