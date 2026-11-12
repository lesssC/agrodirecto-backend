package pe.agrodirecto.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de negocio y seguridad configurables en application.properties (prefijo {@code agrodirecto}).
 */
@ConfigurationProperties(prefix = "agrodirecto")
public record AgroDirectoProperties(
        /* RN-A4: comisión que paga el comprador; nunca se descuenta al agricultor. */
        BigDecimal comisionPorcentaje,
        /* RN-15: horas que tiene el comprador para reclamar tras la entrega. */
        int horasReclamo,
        /* RN-C2: horas tras la entrega para liberar el pago si el comprador no confirma ni reclama. */
        int horasLiberacionAutomatica,
        /* Horas que tiene el productor para aceptar o rechazar un pedido pagado. */
        int horasParaAceptar,
        /* Minutos para pagar un pedido registrado; luego se cancela y el stock vuelve a la venta. */
        int minutosParaPagar,
        /* RN-A3: porcentaje por debajo del precio mayorista que dispara la alerta al agricultor. */
        BigDecimal alertaPrecioBajoPorcentaje,
        CuentaNueva cuentaNueva,
        Seguridad seguridad,
        String secretoWebhookSms,
        List<String> origenesPermitidos) {

    /** RN-18: límites para compradores que aún no tienen historial. */
    public record CuentaNueva(int pedidosParaConfianza, BigDecimal montoMaximo, int pedidosAbiertosMaximo) {
    }

    /**
     * @param jwtSecreto      clave HMAC de al menos 32 caracteres para firmar los JWT
     * @param horasToken      vigencia del token de acceso
     * @param intentosMaximos intentos fallidos antes del bloqueo temporal
     * @param minutosBloqueo  duración del bloqueo
     * @param claveCifrado    clave para cifrar los secretos TOTP
     */
    public record Seguridad(String jwtSecreto, int horasToken, int intentosMaximos, int minutosBloqueo,
            /* Clave para cifrar los secretos TOTP en la base de datos (AES-256-GCM). */
            String claveCifrado,
            /* Roles obligados a usar el segundo factor. */
            java.util.Set<pe.agrodirecto.usuarios.dominio.Rol> rolesConDobleFactor,
            /* Configura secretos TOTP conocidos en las cuentas demo. Desactivar en producción. */
            boolean dobleFactorDemo) {
    }
}
