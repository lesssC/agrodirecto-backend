package pe.agrodirecto.catalogo.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Precio de mercado por kilo de un cultivo (RN-A2). Sirve para dos cosas: mostrar al comprador que paga
 * un precio justo y avisar al agricultor si está vendiendo por debajo del mercado (RN-A3).
 */
@Entity
@Table(name = "precio_referencia")
@Getter
@Setter
@NoArgsConstructor
public class PrecioReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "precio_mayorista_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMayoristaKg;

    @Column(name = "precio_minorista_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMinoristaKg;

    @Column(nullable = false, length = 160)
    private String fuente;

    @Column(nullable = false)
    private LocalDate fecha;
}
