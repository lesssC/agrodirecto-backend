package pe.agrodirecto.usuarios.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitudes y respuestas de la verificación en dos pasos. */
public final class DobleFactorDtos {

    private DobleFactorDtos() {
    }

    public record CodigoRequest(@NotBlank @Size(max = 20) String codigo) {
    }

    public record VerificacionRequest(@NotBlank String tokenTemporal, @NotBlank @Size(max = 20) String codigo) {
    }

    /** {@code uri} se convierte en código QR en el frontend; {@code secreto} sirve para ingresarlo a mano. */
    public record ConfiguracionResponse(String secreto, String uri, String instrucciones) {
    }

    /** Los códigos de respaldo se muestran una sola vez. */
    public record ActivacionResponse(List<String> codigosRespaldo, SesionResponse sesion) {
    }

    public record EstadoResponse(boolean activo, boolean obligatorio, long codigosRespaldoDisponibles) {
    }
}
