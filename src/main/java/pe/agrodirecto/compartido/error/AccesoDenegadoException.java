package pe.agrodirecto.compartido.error;

import lombok.Getter;

/** El usuario está autenticado pero su rol, su estado o su relación con el recurso no le permiten la operación. */
@Getter
public class AccesoDenegadoException extends RuntimeException {

    private final String regla;

    public AccesoDenegadoException(String mensaje) {
        this("RN-07", mensaje);
    }

    public AccesoDenegadoException(String regla, String mensaje) {
        super(mensaje);
        this.regla = regla;
    }
}
