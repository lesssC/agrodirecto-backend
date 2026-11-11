package pe.agrodirecto.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de negocio configurables en application.properties (prefijo {@code agrodirecto}).
 */
@ConfigurationProperties(prefix = "agrodirecto")
public record AgroDirectoProperties(
        /* RN-A4: comisión que paga el comprador; nunca se descuenta al agricultor. */
        BigDecimal comisionPorcentaje,
        /* RN-15: horas que tiene el comprador para reclamar tras la entrega. */
        int horasReclamo,
        /* RN-C2: horas tras la entrega para liberar el pago si el comprador no confirma ni reclama. */
        int horasLiberacionAutomatica,
        /* RN-A3: porcentaje por debajo del precio mayorista que dispara la alerta al agricultor. */
        BigDecimal alertaPrecioBajoPorcentaje,
        int horasSesion,
        CuentaNueva cuentaNueva,
        String secretoWebhookSms,
        List<String> origenesPermitidos) {

    /** RN-18: límites para compradores que aún no tienen historial. */
    public record CuentaNueva(int pedidosParaConfianza, BigDecimal montoMaximo, int pedidosAbiertosMaximo) {
    }
}
