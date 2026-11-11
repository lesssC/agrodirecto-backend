package pe.agrodirecto.compartido.error;

public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " no existe.");
    }
}
