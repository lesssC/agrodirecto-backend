package pe.agrodirecto.productores.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cooperativa o asociación de productores. Puede emitir la factura en nombre del socio sin RUC (RN-C3). */
@Entity
@Table(name = "asociacion")
@Getter
@Setter
@NoArgsConstructor
public class Asociacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 11)
    private String ruc;

    @Column(nullable = false, length = 60)
    private String region;

    @Column(name = "emite_factura", nullable = false)
    private boolean emiteFactura;
}
