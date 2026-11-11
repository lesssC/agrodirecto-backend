package pe.agrodirecto.productores.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;

/**
 * Agricultor que vende en AgroDirecto. Puede tener cuenta propia ({@code usuario}) o no tenerla:
 * en ese caso un agente de campo gestiona su perfil en su nombre (RN-B1). El dinero y la reputación
 * siempre son del agricultor.
 */
@Entity
@Table(name = "productor")
@Getter
@Setter
@NoArgsConstructor
public class Productor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agente_id")
    private Usuario agente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asociacion_id")
    private Asociacion asociacion;

    @Column(name = "nombre_comercial", nullable = false, length = 120)
    private String nombreComercial;

    @Column(name = "nombre_titular", nullable = false, length = 120)
    private String nombreTitular;

    @Column(nullable = false, length = 8)
    private String dni;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 60)
    private String region;

    @Column(nullable = false, length = 60)
    private String provincia;

    @Column(length = 60)
    private String distrito;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(length = 1000)
    private String historia;

    @Column(length = 500)
    private String practicas;

    @Column(name = "tiene_ruc", nullable = false)
    private boolean tieneRuc;

    @Column(length = 11)
    private String ruc;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_cobro", nullable = false, length = 20)
    private MetodoCobro metodoCobro;

    @Column(name = "numero_cobro", nullable = false, length = 30)
    private String numeroCobro;

    /** Identidad comprobada en persona por un agente o por la administración (RN-B5). */
    @Column(nullable = false)
    private boolean verificado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verificado_por")
    private Usuario verificadoPor;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.ACTIVO;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "productor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("diasTransito ASC, region ASC")
    private List<ZonaCobertura> zonas = new ArrayList<>();

    public boolean estaActivo() {
        return estado == EstadoCuenta.ACTIVO;
    }

    /** Quién puede administrar este perfil: el propio agricultor, su agente o la administración. */
    public boolean esGestionadoPor(Usuario usuarioActual) {
        if (usuarioActual.getRol() == Rol.ADMINISTRADOR) {
            return true;
        }
        return mismoUsuario(usuario, usuarioActual) || mismoUsuario(agente, usuarioActual);
    }

    /** RN-C3: puede entregar factura si tiene RUC propio o si su asociación factura por él. */
    public boolean emiteFactura() {
        return tieneRuc || (asociacion != null && asociacion.isEmiteFactura());
    }

    public boolean tieneCuentaPropia() {
        return usuario != null;
    }

    public Optional<ZonaCobertura> zonaPara(String regionDestino) {
        return zonas.stream().filter(z -> z.getRegion().equals(regionDestino)).findFirst();
    }

    public void verificar(Usuario verificador, LocalDateTime fecha) {
        verificado = true;
        verificadoPor = verificador;
        fechaVerificacion = fecha;
    }

    /**
     * Deja exactamente las zonas indicadas. Las regiones que ya existían se actualizan en su lugar
     * (borrarlas y reinsertarlas chocaría con la restricción única productor + región).
     */
    public void reemplazarZonas(List<ZonaCobertura> nuevas) {
        List<String> regionesNuevas = nuevas.stream().map(ZonaCobertura::getRegion).toList();
        zonas.removeIf(zona -> !regionesNuevas.contains(zona.getRegion()));
        for (ZonaCobertura nueva : nuevas) {
            zonaPara(nueva.getRegion()).ifPresentOrElse(existente -> {
                existente.setDiasTransito(nueva.getDiasTransito());
                existente.setPedidoMinimo(nueva.getPedidoMinimo());
                existente.setCostoFlete(nueva.getCostoFlete());
            }, () -> {
                nueva.setProductor(this);
                zonas.add(nueva);
            });
        }
    }

    private static boolean mismoUsuario(Usuario a, Usuario b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }
}
