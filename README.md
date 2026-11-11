# AgroDirecto — Backend

API REST de AgroDirecto, plataforma que conecta a pequeños agricultores del Perú con restaurantes, bodegas y comercios **a precio justo y sin intermediarios**.

El frontend está en el repositorio [`agrodirecto-project`](https://github.com/andrebm99/agrodirecto-project).
El plan de trabajo por tareas está en [ROADMAP.md](ROADMAP.md).

## Tecnologías

- Java 21 y Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security).
- JWT firmado con HS256 (Spring Security OAuth2 Resource Server).
- MySQL 8 y Flyway para versionar el esquema.
- H2 en modo MySQL solo para las pruebas automáticas.
- Docker Compose para la base de datos local.

## Arquitectura: monolito modular

El código se organiza **por módulo de negocio** y cada módulo tiene sus propias capas:

```text
pe.agrodirecto
├── config/          Configuración transversal (CORS, reloj, propiedades de negocio)
├── compartido/      Errores comunes y utilidades de dinero
├── usuarios/        Cuentas, roles y autenticación
├── productores/     Agricultores, agentes, asociaciones y zonas de cobertura
├── catalogo/        Productos, formatos de venta, precios de referencia y favoritos
├── pedidos/         Cotización, pedidos, estados e historial
├── pagos/           Pago retenido y liberación al agricultor
├── notificaciones/  WhatsApp/SMS al agricultor y avisos web
├── confianza/       Reseñas, reputación y disputas
└── administracion/  Métricas y control de la plataforma

Cada módulo:
  dominio/       Entidades JPA y enums con sus reglas internas
  repositorio/   Acceso a datos (Spring Data JPA)
  servicio/      Casos de uso y reglas de negocio (transaccionales)
  web/           Controladores REST y DTOs de entrada/salida
```

Las dependencias van en un solo sentido, `web → servicio → repositorio → dominio`, y entre módulos no hay ciclos. Por ejemplo, `pedidos` usa `catalogo` y `productores`, pero ninguno de ellos conoce a `pedidos`.

## Ejecución local

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d        # MySQL 8.4 en el puerto 3307
./mvnw spring-boot:run      # API en http://localhost:8080
```

Al arrancar, Flyway crea las tablas (`db/migration`) y carga los datos de demostración (`db/demo`).

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/agrodirecto?...` |
| `DB_USER` / `DB_PASSWORD` | `agrodirecto` / `agrodirecto` |
| `PORT` | `8080` |
| `JWT_SECRET` | Clave de desarrollo (cambiar en producción) |

## Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | Público | Crea una cuenta de comprador, productor o agente |
| POST | `/api/auth/login` | Público | Devuelve el JWT y los datos del usuario |
| GET | `/api/auth/yo` | Con token | Datos de la cuenta de la sesión |

```bash
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"compras@saborandino.pe","password":"demo1234"}'
# Usar el token recibido en las demás peticiones:
curl localhost:8080/api/auth/yo -H 'Authorization: Bearer <token>'
```

- El JWT dura 12 horas y lleva el id del usuario (`sub`) y su `rol`. Spring Security convierte el rol en `ROLE_<rol>` para autorizar las rutas: por ejemplo, `/api/admin/**` solo admite administradores.
- Tras 5 intentos fallidos, la cuenta se bloquea 15 minutos (protección contra fuerza bruta).
- Una cuenta suspendida pierde el acceso de inmediato, aunque su token siga vigente (RN-23).
- No se permiten dos cuentas con el mismo correo, celular o DNI (RN-23), y nadie puede registrarse como administrador (RN-07).
- En producción hay que definir la variable `JWT_SECRET`, de al menos 32 caracteres.

## Productores

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/productores?region=&atiende=` | Público | Directorio de agricultores. `atiende` filtra por la región a la que entregan |
| GET | `/api/productores/{id}` | Público | Perfil público: historia, zona aproximada y zonas de cobertura |
| GET | `/api/regiones`, `/api/asociaciones` | Público | Catálogos para los formularios |
| GET | `/api/gestion/productores` | Productor, agente, admin | Perfiles que el usuario administra |
| POST | `/api/gestion/productores/mi-perfil` | Productor | El agricultor con smartphone crea su perfil |
| POST | `/api/gestion/productores/asistidos` | Agente verificado | Registro de un agricultor sin computadora (RN-B1) |
| PUT | `/api/gestion/productores/{id}` | Quien lo gestiona | Actualiza el perfil |
| POST | `/api/gestion/productores/{id}/verificar` | Su agente o admin | Verificación presencial del DNI (RN-B5) |
| PUT | `/api/gestion/productores/{id}/zonas` | Quien lo gestiona | Regiones que abastece, con días de tránsito, pedido mínimo y flete (RN-09) |

- El perfil público no muestra DNI, celular ni datos de cobro (RN-24), y la ubicación se redondea a unos 1 km (RN-06).
- El medio de cobro se valida según su tipo: Yape o Plin (celular), Cuenta DNI del Banco de la Nación (DNI) o cuenta bancaria (CCI) (RN-C2).
- Si cambia el DNI de un perfil, su verificación se anula.
- Un agricultor sin RUC puede facturar a través de su cooperativa (`emiteFactura`, RN-C3).

### Cuentas de demostración

Contraseña de todas: `demo1234`.

| Correo | Rol |
|---|---|
| `admin@agrodirecto.pe` | Administrador |
| `compras@saborandino.pe` | Comprador verificado (empresa con RUC) |
| `donlucho@correo.pe` | Comprador nuevo (sujeto a límites de cuenta nueva) |
| `andesverde@agrodirecto.pe` | Productor con cuenta propia |
| `promotora@agrodirecto.pe` | Agente de campo que gestiona a cinco agricultores sin computadora |

## Pruebas

```bash
./mvnw test
```

## Equipo

Desarrollo Full Stack — UTP, sección 45554, 2026.
