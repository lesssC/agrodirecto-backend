package pe.agrodirecto.administracion.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.administracion.web.ResumenResponse;
import pe.agrodirecto.compartido.Dinero;

/**
 * Indicadores del panel. Son consultas de reporte, por eso se escriben en SQL estándar (funcionan en MySQL y H2)
 * en lugar de cargar entidades.
 */
@Service
@RequiredArgsConstructor
public class MetricasService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        BigDecimal[] dinero = jdbc.queryForObject("""
                SELECT COALESCE(SUM(total), 0), COALESCE(SUM(monto_productor), 0), COALESCE(SUM(comision), 0)
                FROM pedido WHERE estado = 'COMPLETADO'""",
                (rs, i) -> new BigDecimal[] {rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)});
        BigDecimal transado = Dinero.redondear(dinero[0]);
        BigDecimal pagado = Dinero.redondear(dinero[1]);
        BigDecimal porcentaje = transado.signum() == 0 ? null
                : pagado.multiply(Dinero.CIEN).divide(transado, 1, RoundingMode.HALF_UP);

        return new ResumenResponse(
                conteoPorGrupo("SELECT rol, COUNT(*) FROM usuario GROUP BY rol ORDER BY rol"),
                new ResumenResponse.Pendientes(
                        contar("SELECT COUNT(*) FROM usuario WHERE rol = 'AGENTE' AND verificado = FALSE"),
                        contar("SELECT COUNT(*) FROM usuario WHERE rol = 'COMPRADOR' AND verificado = FALSE"),
                        contar("SELECT COUNT(*) FROM productor WHERE verificado = FALSE AND estado = 'ACTIVO'")),
                contar("SELECT COUNT(*) FROM productor"),
                contar("SELECT COUNT(*) FROM productor WHERE verificado = TRUE"),
                contar("SELECT COUNT(*) FROM producto WHERE estado = 'ACTIVO'"),
                conteoPorGrupo("SELECT estado, COUNT(*) FROM pedido GROUP BY estado ORDER BY estado"),
                new ResumenResponse.Dinero(transado, pagado, Dinero.redondear(dinero[2]),
                        Dinero.redondear(jdbc.queryForObject(
                                "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE estado = 'RETENIDO'", BigDecimal.class)),
                        porcentaje),
                contar("SELECT COUNT(*) FROM disputa WHERE estado = 'ABIERTA'"),
                topProductores());
    }

    private List<ResumenResponse.ProductorDestacado> topProductores() {
        return jdbc.query("""
                SELECT pr.id, pr.nombre_comercial, SUM(p.monto_productor) AS ventas, COUNT(*) AS pedidos
                FROM pedido p JOIN productor pr ON pr.id = p.productor_id
                WHERE p.estado = 'COMPLETADO'
                GROUP BY pr.id, pr.nombre_comercial
                ORDER BY ventas DESC
                LIMIT 5""", (rs, i) -> new ResumenResponse.ProductorDestacado(rs.getLong(1), rs.getString(2),
                Dinero.redondear(rs.getBigDecimal(3)), rs.getLong(4)));
    }

    private Map<String, Long> conteoPorGrupo(String sql) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            resultado.put(rs.getString(1), rs.getLong(2));
        });
        return resultado;
    }

    private long contar(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
