package pe.agrodirecto.notificaciones.dominio;

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

/** Un mensaje enviado (SMS o WhatsApp al agricultor) o un aviso web para un usuario. */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "productor_id")
    private Long productorId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal;

    /** Celular de destino para SMS y WhatsApp. */
    @Column(length = 20)
    private String destino;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(length = 700)
    private String enlace;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}
