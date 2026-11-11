package pe.agrodirecto.catalogo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import pe.agrodirecto.PruebaIntegracion;

class CatalogoControllerTest extends PruebaIntegracion {

    // ---------- Catálogo público ----------

    @Test
    void elCatalogoEsPublicoYMuestraElPrecioDesde() throws Exception {
        getCon(null, "/api/productos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[0].nombre").value("Papa amarilla seleccionada"))
                .andExpect(jsonPath("$[0].precioDesde").value(14.00))
                .andExpect(jsonPath("$[0].formatoDesde").value("Bolsa de 5 kg"))
                .andExpect(jsonPath("$[0].productor.nombreComercial").value("AndesVerde"));
    }

    @Test
    void buscaSinImportarTildesNiMayusculas() throws Exception {
        getCon(null, "/api/productos?q=CAFE")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].categoria", everyItem(org.hamcrest.Matchers.is("Granos"))));
        getCon(null, "/api/productos?categoria=tuberculos").andExpect(jsonPath("$.length()").value(3));
        getCon(null, "/api/productos?q=puno").andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void filtraPorRegionDeEntregaOrganicoYOrdenaPorPrecio() throws Exception {
        getCon(null, "/api/productos?atiende=Cusco")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].productor.nombreComercial", everyItem(org.hamcrest.Matchers.is("ValleFresco"))));
        getCon(null, "/api/productos?organico=true").andExpect(jsonPath("$.length()").value(7));
        getCon(null, "/api/productos?orden=precio_desc")
                .andExpect(jsonPath("$[0].nombre").value("Papa canchán"))
                .andExpect(jsonPath("$[0].precioDesde").value(25.00));
        getCon(null, "/api/productos?atiende=Atlantida").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laFichaComparaConElPrecioDeMercado() throws Exception {
        getCon(null, "/api/productos/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formatos.length()").value(3))
                .andExpect(jsonPath("$.formatos[0].nombre").value("Bolsa de 5 kg"))
                .andExpect(jsonPath("$.formatos[0].precioPorKg").value(2.80))
                .andExpect(jsonPath("$.formatos[2].descuento.desdeCantidad").value(10))
                .andExpect(jsonPath("$.precioReferencia.precioMinoristaKg").value(4.50))
                .andExpect(jsonPath("$.productor.nombreComercial").value("AndesVerde"))
                .andExpect(jsonPath("$.productor.dni").doesNotExist());
    }

    @Test
    void listaCategoriasYPreciosDeReferencia() throws Exception {
        getCon(null, "/api/categorias").andExpect(jsonPath("$.length()").value(4));
        getCon(null, "/api/precios-referencia").andExpect(jsonPath("$.length()").value(10));
    }

    // ---------- Publicación ----------

    @Test
    void elProductorGuardaUnBorradorConAlertaDePrecioBajoYLuegoLoPublica() throws Exception {
        String productor = token(PRODUCTOR);
        String cuerpo = postCon(productor, "/api/gestion/productos", papaNativa(idProductor("40000004"), false, "100.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.alertas.length()").value(1))
                .andExpect(jsonPath("$.alertas[0]", containsString("29% debajo del precio mayorista")))
                .andReturn().getResponse().getContentAsString();
        Integer id = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.id");

        getCon(null, "/api/productos/" + id).andExpect(status().isNotFound());

        cambiarEstado(productor, id, "ACTIVO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        nuevaPeticion();
        getCon(null, "/api/productos?q=papa nativa").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unPrecioJustoNoGeneraAlertas() throws Exception {
        postCon(token(PRODUCTOR), "/api/gestion/productos", papaNativa(idProductor("40000004"), true, "150.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.alertas.length()").value(0));
    }

    @Test
    void aceptaUnProductoConLosCamposMinimos() throws Exception {
        postCon(token(PRODUCTOR), "/api/gestion/productos", """
                {"productorId":%d,"categoriaId":1,"nombre":"Papa mínima",
                 "formatos":[{"nombre":"Saco de 50 kg","tipo":"MAYORISTA","precio":150,"stock":5}]}"""
                .formatted(idProductor("40000004")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organico").value(false))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void unAgricultorSinVerificarSoloPuedeGuardarBorradores() throws Exception {
        String agente = token(AGENTE);
        Long selvaNorte = idProductor("40000015");
        postCon(agente, "/api/gestion/productos", papaNativa(selvaNorte, true, "150.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-B5"));
        postCon(agente, "/api/gestion/productos", papaNativa(selvaNorte, false, "150.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void soloQuienGestionaAlProductorPuedePublicarPorEl() throws Exception {
        postCon(token(PRODUCTOR), "/api/gestion/productos", papaNativa(idProductor("40000011"), false, "150.00"))
                .andExpect(status().isForbidden());
        postCon(token(COMPRADOR), "/api/gestion/productos", papaNativa(idProductor("40000004"), false, "150.00"))
                .andExpect(status().isForbidden());
        postCon(null, "/api/gestion/productos", papaNativa(idProductor("40000004"), false, "150.00"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validaLasPresentacionesDeVenta() throws Exception {
        String productor = token(PRODUCTOR);
        Long andesVerde = idProductor("40000004");
        postCon(productor, "/api/gestion/productos", papaNativa(andesVerde, false, "150.00")
                .replace("\"Arroba de 12.5 kg\"", "\"saco de 50 KG\""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-02"));
        postCon(productor, "/api/gestion/productos", papaNativa(andesVerde, false, "150.00")
                .replace(",\"porcentajeDescuento\":5", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-A1"));
        postCon(productor, "/api/gestion/productos", """
                {"productorId":%d,"categoriaId":1,"nombre":"Sin formatos","formatos":[]}""".formatted(andesVerde))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasPresentacionesOmitidasSeDesactivanSinBorrarse() throws Exception {
        Long saco = jdbc.queryForObject(
                "SELECT id FROM formato_venta WHERE producto_id = 1 AND nombre = 'Saco de 50 kg'", Long.class);
        putCon(token(PRODUCTOR), "/api/gestion/productos/1", """
                {"productorId":%d,"categoriaId":1,"precioReferenciaId":1,"nombre":"Papa amarilla seleccionada",
                 "organico":true,"publicar":true,
                 "formatos":[{"id":%d,"nombre":"Saco de 50 kg","tipo":"MAYORISTA","pesoKg":50,"precio":130,"stock":18}]}"""
                .formatted(idProductor("40000004"), saco))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formatos.length()").value(3))
                .andExpect(jsonPath("$.alertas.length()").value(0));

        nuevaPeticion();
        getCon(null, "/api/productos/1")
                .andExpect(jsonPath("$.formatos.length()").value(1))
                .andExpect(jsonPath("$.formatos[0].precio").value(130.00));
    }

    @Test
    void unProductoRetiradoDesapareceDelCatalogo() throws Exception {
        cambiarEstado(token(PRODUCTOR), 1, "RETIRADO").andExpect(status().isOk());
        nuevaPeticion();
        getCon(null, "/api/productos")
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[*].id", not(hasItem(1))));
        getCon(null, "/api/productos/1").andExpect(status().isNotFound());
    }

    @Test
    void elAgenteVeLosProductosDeTodosSusAgricultores() throws Exception {
        getCon(token(AGENTE), "/api/gestion/productos")
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[*].productor", not(hasItem("AndesVerde"))));
    }

    // ---------- Favoritos ----------

    @Test
    void guardaYQuitaFavoritosSinDuplicar() throws Exception {
        String comprador = token(COMPRADOR);
        putCon(comprador, "/api/favoritos/3", "").andExpect(status().isNoContent());
        putCon(comprador, "/api/favoritos/3", "").andExpect(status().isNoContent());
        getCon(comprador, "/api/favoritos")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Palta Hass"));

        mvc.perform(delete("/api/favoritos/3").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isNoContent());
        getCon(comprador, "/api/favoritos").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void losFavoritosRequierenSesionYUnProductoVisible() throws Exception {
        getCon(null, "/api/favoritos").andExpect(status().isUnauthorized());
        putCon(token(COMPRADOR), "/api/favoritos/999", "").andExpect(status().isNotFound());
    }

    // ---------- Apoyo ----------

    private ResultActions cambiarEstado(String token, Integer id, String estado) throws Exception {
        return mvc.perform(patch("/api/gestion/productos/" + id + "/estado")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}"));
    }

    /** Papa nativa con tres presentaciones; el saco de 50 kg lleva el precio indicado. */
    private static String papaNativa(Long productorId, boolean publicar, String precioSaco) {
        return """
                {"productorId":%d,"categoriaId":1,"precioReferenciaId":1,"nombre":"Papa nativa huayro",
                 "descripcion":"Papa nativa de altura.","imagenUrl":"/images/products/potatoes.jpg","organico":true,
                 "fechaCosecha":"2026-11-02","vidaUtilDias":60,"calibre":"Primera","publicar":%s,
                 "formatos":[
                   {"nombre":"Saco de 50 kg","tipo":"MAYORISTA","pesoKg":50,"precio":%s,"stock":10,
                    "cantidadMinDescuento":10,"porcentajeDescuento":5},
                   {"nombre":"Arroba de 12.5 kg","tipo":"INTERMEDIO","pesoKg":12.5,"precio":40,"stock":20},
                   {"nombre":"Bolsa de 5 kg","tipo":"PEQUENO","pesoKg":5,"precio":17,"stock":30}]}"""
                .formatted(productorId, publicar, precioSaco);
    }
}
