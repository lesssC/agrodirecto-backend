package pe.agrodirecto.catalogo.dominio;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorito")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Favorito {

    @EmbeddedId
    private Clave id;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Clave implements Serializable {

        @Column(name = "usuario_id")
        private Long usuarioId;

        @Column(name = "producto_id")
        private Long productoId;
    }
}
