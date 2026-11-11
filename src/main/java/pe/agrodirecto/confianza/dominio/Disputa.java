package pe.agrodirecto.confianza.dominio;

import java.time.LocalDateTime;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.usuarios.dominio.Usuario;

/**
 * RN-22: reclamo del comprador sobre un pedido entregado. No hay reembolso automático: el agricultor puede
 * presentar su descargo y la administración decide con la evidencia de ambas partes.
 */
@Entity
@Table(name = "disputa")
@Getter
@Setter
@NoArgsConstructor
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abierta_por")
    private Usuario abiertaPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MotivoDisputa motivo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;

    @Column(length = 1000)
    private String descargo;

    @Column(name = "evidencia_descargo_url", length = 500)
    private String evidenciaDescargoUrl;

    @Column(name = "descargo_en")
    private LocalDateTime descargoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDisputa estado = EstadoDisputa.ABIERTA;

    @Column(length = 1000)
    private String resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por")
    private Usuario resueltaPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    public void registrarDescargo(String texto, String evidencia, LocalDateTime ahora) {
        exigirAbierta();
        descargo = texto;
        evidenciaDescargoUrl = evidencia;
        descargoEn = ahora;
    }

    public void resolver(EstadoDisputa resultado, String texto, Usuario administrador, LocalDateTime ahora) {
        exigirAbierta();
        estado = resultado;
        resolucion = texto;
        resueltaPor = administrador;
        resueltaEn = ahora;
    }

    private void exigirAbierta() {
        if (estado != EstadoDisputa.ABIERTA) {
            throw new ReglaNegocioException("RN-22", "La disputa ya fue resuelta.");
        }
    }
}
