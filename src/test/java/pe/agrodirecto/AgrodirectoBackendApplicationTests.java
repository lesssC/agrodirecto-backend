package pe.agrodirecto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AgrodirectoBackendApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void cargaElContextoYAplicaLasMigraciones() {
        assertThat(contar("usuario")).isEqualTo(5);
        assertThat(contar("productor")).isEqualTo(6);
        assertThat(contar("producto")).isEqualTo(16);
        assertThat(contar("formato_venta")).isEqualTo(37);
    }

    @Test
    void losProductosConservanLosIdsDelFrontend() {
        String nombre = jdbc.queryForObject("SELECT nombre FROM producto WHERE id = 16", String.class);
        assertThat(nombre).isEqualTo("Café molido de altura");
    }

    @Test
    void cadaProductoTieneAlMenosUnFormatoDeVenta() {
        Integer sinFormato = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto p WHERE NOT EXISTS (SELECT 1 FROM formato_venta f WHERE f.producto_id = p.id)",
                Integer.class);
        assertThat(sinFormato).isZero();
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }
}
