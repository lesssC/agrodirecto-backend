package pe.agrodirecto.compartido.error;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, e.getRegla(), e.getMessage(), List.of());
    }

    @ExceptionHandler(NoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, null, e.getMessage(), List.of());
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ResponseEntity<ErrorRespuesta> noAutenticado(NoAutenticadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, null, e.getMessage(), List.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ErrorRespuesta> accesoDenegado(AccesoDenegadoException e) {
        return responder(HttpStatus.FORBIDDEN, "RN-07", e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, null, "Los datos enviados no son válidos.", detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorRespuesta> formato(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, null, "La solicitud tiene un formato incorrecto.", List.of());
    }

    /** Dos compradores intentaron reservar el mismo stock a la vez. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorRespuesta> concurrencia(OptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "RN-03",
                "El stock cambió mientras se procesaba tu pedido. Vuelve a intentarlo.", List.of());
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String regla, String mensaje, List<String> detalles) {
        return ResponseEntity.status(estado).body(ErrorRespuesta.de(estado.value(), regla, mensaje, detalles));
    }
}
