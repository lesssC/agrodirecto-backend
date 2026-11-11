package pe.agrodirecto.compartido.error;

import lombok.Getter;

/** Se lanza cuando una operación viola una regla de negocio documentada (RN-xx). */
@Getter
public class ReglaNegocioException extends RuntimeException {

    private final String regla;

    public ReglaNegocioException(String regla, String mensaje) {
        super(mensaje);
        this.regla = regla;
    }
}
