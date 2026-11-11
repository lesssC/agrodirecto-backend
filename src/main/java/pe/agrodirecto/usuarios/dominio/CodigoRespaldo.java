package pe.agrodirecto.usuarios.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "codigo_respaldo")
@Getter
@Setter
@NoArgsConstructor
public class CodigoRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** SHA-256 del código: el código en claro solo se muestra una vez al activarlo. */
    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;
}
