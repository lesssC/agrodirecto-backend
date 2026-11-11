package pe.agrodirecto.productores.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.Dinero;
import pe.agrodirecto.compartido.Regiones;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.dominio.ZonaCobertura;
import pe.agrodirecto.productores.repositorio.AsociacionRepository;
import pe.agrodirecto.productores.repositorio.ProductorRepository;
import pe.agrodirecto.productores.web.AsociacionResponse;
import pe.agrodirecto.productores.web.ProductorGestionResponse;
import pe.agrodirecto.productores.web.ProductorPublicoResponse;
import pe.agrodirecto.productores.web.ProductorRequest;
import pe.agrodirecto.productores.web.ZonaRequest;
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;

@Service
@RequiredArgsConstructor
public class ProductorService {

    // Límites aproximados del territorio peruano, para descartar coordenadas mal ingresadas.
    private static final BigDecimal LAT_MIN = new BigDecimal("-18.5");
    private static final BigDecimal LAT_MAX = new BigDecimal("0.1");
    private static final BigDecimal LNG_MIN = new BigDecimal("-81.5");
    private static final BigDecimal LNG_MAX = new BigDecimal("-68.5");

    private final ProductorRepository productores;
    private final AsociacionRepository asociaciones;
    private final UsuarioActualService usuarioActual;
    private final Clock reloj;

    // ---------- Consultas públicas ----------

    @Transactional(readOnly = true)
    public List<ProductorPublicoResponse> directorio(String region, String atiende) {
        Optional<String> origen = filtroRegion(region);
        Optional<String> destino = filtroRegion(atiende);
        if ((region != null && origen.isEmpty()) || (atiende != null && destino.isEmpty())) {
            return List.of();
        }
        return productores.buscarDirectorio(EstadoCuenta.ACTIVO, origen.orElse(null), destino.orElse(null))
                .stream().map(ProductorPublicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductorPublicoResponse detallePublico(Long id) {
        Productor productor = buscar(id);
        if (!productor.estaActivo()) {
            throw new NoEncontradoException("Productor", id);
        }
        return ProductorPublicoResponse.de(productor);
    }

    @Transactional(readOnly = true)
    public List<AsociacionResponse> asociaciones() {
        return asociaciones.findAllByOrderByNombre().stream().map(AsociacionResponse::de).toList();
    }

    // ---------- Gestión ----------

    /** Perfiles que el usuario puede administrar: el suyo, los de sus agricultores o todos (administración). */
    @Transactional(readOnly = true)
    public List<ProductorGestionResponse> gestionados(Jwt jwt) {
        Usuario actual = usuarioActual.exigirRol(jwt, Rol.PRODUCTOR, Rol.AGENTE, Rol.ADMINISTRADOR);
        List<Productor> lista = switch (actual.getRol()) {
            case PRODUCTOR -> productores.findByUsuarioId(actual.getId()).map(List::of).orElse(List.of());
            case AGENTE -> productores.findByAgenteIdOrderByNombreComercial(actual.getId());
            default -> productores.findAllByOrderByNombreComercial();
        };
        return lista.stream().map(ProductorGestionResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductorGestionResponse detalleGestion(Jwt jwt, Long id) {
        return ProductorGestionResponse.de(buscarGestionable(jwt, id));
    }

    /** El agricultor con smartphone crea su propio perfil. */
    @Transactional
    public ProductorGestionResponse crearPerfilPropio(Jwt jwt, ProductorRequest solicitud) {
        Usuario actual = usuarioActual.exigirRol(jwt, Rol.PRODUCTOR);
        if (productores.existsByUsuarioId(actual.getId())) {
            throw new ReglaNegocioException("RN-23", "Tu cuenta ya tiene un perfil de productor.");
        }
        if (actual.getDni() != null && !actual.getDni().equals(solicitud.dni())) {
            throw new ReglaNegocioException("RN-23", "El DNI del perfil debe ser el mismo de tu cuenta.");
        }
        Productor productor = nuevo(solicitud);
        productor.setUsuario(actual);
        return ProductorGestionResponse.de(productores.save(productor));
    }

    /**
     * RN-B1: el agente registra a un agricultor que no tiene computadora ni correo.
     * Solo pueden hacerlo agentes ya verificados por AgroDirecto (RN-17).
     */
    @Transactional
    public ProductorGestionResponse registrarAsistido(Jwt jwt, ProductorRequest solicitud) {
        Usuario agente = usuarioActual.exigirRol(jwt, Rol.AGENTE);
        if (!agente.isVerificado()) {
            throw new ReglaNegocioException("RN-17",
                    "Tu cuenta de agente aún no ha sido verificada por AgroDirecto; no puedes registrar agricultores.");
        }
        Productor productor = nuevo(solicitud);
        productor.setAgente(agente);
        return ProductorGestionResponse.de(productores.save(productor));
    }

    @Transactional
    public ProductorGestionResponse actualizar(Jwt jwt, Long id, ProductorRequest solicitud) {
        Productor productor = buscarGestionable(jwt, id);
        boolean cambiaIdentidad = !productor.getDni().equals(solicitud.dni());
        if (cambiaIdentidad && productores.existsByDni(solicitud.dni())) {
            throw new ReglaNegocioException("RN-23", "Ya existe un productor registrado con ese DNI.");
        }
        aplicarDatos(productor, solicitud);
        if (cambiaIdentidad) {
            // Si cambia la identidad, la verificación anterior deja de ser válida.
            productor.setVerificado(false);
            productor.setVerificadoPor(null);
            productor.setFechaVerificacion(null);
        }
        return ProductorGestionResponse.de(productor);
    }

    /**
     * RN-B5: verificación presencial. La hace el agente que conoce al agricultor (comprueba su DNI en persona)
     * o la administración. Un productor no puede verificarse a sí mismo.
     */
    @Transactional
    public ProductorGestionResponse verificar(Jwt jwt, Long id) {
        Usuario actual = usuarioActual.exigirRol(jwt, Rol.AGENTE, Rol.ADMINISTRADOR);
        Productor productor = buscar(id);
        if (!productor.esGestionadoPor(actual)) {
            throw new AccesoDenegadoException("Solo el agente de este agricultor o la administración pueden verificarlo.");
        }
        if (actual.getRol() == Rol.AGENTE && !actual.isVerificado()) {
            throw new ReglaNegocioException("RN-17", "Un agente sin verificar no puede verificar a otros.");
        }
        productor.verificar(actual, LocalDateTime.now(reloj));
        return ProductorGestionResponse.de(productor);
    }

    /** RN-09: define a qué regiones abastece, en cuántos días, con qué pedido mínimo y flete. */
    @Transactional
    public ProductorGestionResponse reemplazarZonas(Jwt jwt, Long id, List<ZonaRequest> solicitud) {
        Productor productor = buscarGestionable(jwt, id);
        Set<String> vistas = new HashSet<>();
        List<ZonaCobertura> zonas = solicitud.stream().map(z -> {
            String region = regionValida(z.region(), "RN-09");
            if (!vistas.add(region)) {
                throw new ReglaNegocioException("RN-09", "La región " + region + " está repetida.");
            }
            ZonaCobertura zona = new ZonaCobertura();
            zona.setRegion(region);
            zona.setDiasTransito(z.diasTransito());
            zona.setPedidoMinimo(Dinero.redondear(z.pedidoMinimo()));
            zona.setCostoFlete(Dinero.redondear(z.costoFlete()));
            return zona;
        }).toList();
        productor.reemplazarZonas(zonas);
        productores.flush();
        return ProductorGestionResponse.de(productor);
    }

    // ---------- Apoyo para otros módulos ----------

    /** Busca un productor que el usuario del token puede administrar; usado también por catálogo y pedidos. */
    @Transactional(readOnly = true)
    public Productor buscarGestionable(Jwt jwt, Long id) {
        Usuario actual = usuarioActual.exigirRol(jwt, Rol.PRODUCTOR, Rol.AGENTE, Rol.ADMINISTRADOR);
        Productor productor = buscar(id);
        if (!productor.esGestionadoPor(actual)) {
            throw new AccesoDenegadoException("No gestionas el perfil de este productor.");
        }
        return productor;
    }

    public Productor buscar(Long id) {
        return productores.findById(id).orElseThrow(() -> new NoEncontradoException("Productor", id));
    }

    // ---------- Internos ----------

    private Productor nuevo(ProductorRequest solicitud) {
        if (productores.existsByDni(solicitud.dni())) {
            throw new ReglaNegocioException("RN-23", "Ya existe un productor registrado con ese DNI.");
        }
        Productor productor = new Productor();
        productor.setCreadoEn(LocalDateTime.now(reloj));
        productor.setVerificado(false);
        aplicarDatos(productor, solicitud);
        return productor;
    }

    private void aplicarDatos(Productor p, ProductorRequest s) {
        if (!s.metodoCobro().numeroValido(s.numeroCobro())) {
            throw new ReglaNegocioException("RN-C2",
                    "Para " + s.metodoCobro() + " el número de cobro debe ser " + s.metodoCobro().descripcionFormato() + ".");
        }
        validarCoordenadas(s.latitud(), s.longitud());
        p.setNombreComercial(s.nombreComercial().trim());
        p.setNombreTitular(s.nombreTitular().trim());
        p.setDni(s.dni());
        p.setTelefono(s.telefono());
        p.setRegion(regionValida(s.region(), null));
        p.setProvincia(s.provincia().trim());
        p.setDistrito(s.distrito());
        p.setLatitud(s.latitud());
        p.setLongitud(s.longitud());
        p.setHistoria(s.historia());
        p.setPracticas(s.practicas());
        p.setRuc(s.ruc());
        p.setTieneRuc(s.ruc() != null);
        p.setMetodoCobro(s.metodoCobro());
        p.setNumeroCobro(s.numeroCobro());
        p.setAsociacion(s.asociacionId() == null ? null
                : asociaciones.findById(s.asociacionId())
                        .orElseThrow(() -> new NoEncontradoException("Asociación", s.asociacionId())));
    }

    private static void validarCoordenadas(BigDecimal latitud, BigDecimal longitud) {
        if ((latitud == null) != (longitud == null)) {
            throw new ReglaNegocioException("RN-06", "Envía latitud y longitud juntas, o ninguna.");
        }
        if (latitud != null && (fuera(latitud, LAT_MIN, LAT_MAX) || fuera(longitud, LNG_MIN, LNG_MAX))) {
            throw new ReglaNegocioException("RN-06", "La ubicación debe estar dentro del Perú.");
        }
    }

    private static boolean fuera(BigDecimal valor, BigDecimal min, BigDecimal max) {
        return valor.compareTo(min) < 0 || valor.compareTo(max) > 0;
    }

    private static String regionValida(String region, String regla) {
        return Regiones.normalizar(region).orElseThrow(() -> new ReglaNegocioException(regla,
                "La región \"" + region + "\" no existe. Usa una de las 25 regiones del Perú."));
    }

    private static Optional<String> filtroRegion(String region) {
        return Objects.isNull(region) || region.isBlank() ? Optional.empty() : Regiones.normalizar(region);
    }
}
