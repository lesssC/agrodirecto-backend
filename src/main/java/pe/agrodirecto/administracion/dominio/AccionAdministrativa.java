package pe.agrodirecto.administracion.dominio;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.agrodirecto.usuarios.dominio.Usuario;

/** Auditoría: la administración también rinde cuentas de cada sanción, verificación o cambio de precios. */
@Entity
@Table(name = "accion_administrativa")
@Getter
@Setter
@NoArgsConstructor
public class AccionAdministrativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id")
    private Usuario administrador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAccion tipo;

    @Column(nullable = false, length = 30)
    private String objetivo;

    @Column(name = "objetivo_id", nullable = false)
    private Long objetivoId;

    @Column(length = 500)
    private String detalle;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
