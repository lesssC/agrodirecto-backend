# AgroDirecto — Backend

API REST de AgroDirecto, plataforma que conecta a pequeños agricultores del Perú con restaurantes, bodegas y comercios **a precio justo y sin intermediarios**.

El frontend está en el repositorio [`agrodirecto-project`](https://github.com/andrebm99/agrodirecto-project).
El plan de trabajo por tareas está en [ROADMAP.md](ROADMAP.md).

## Tecnologías

- Java 21 y Spring Boot 4.1 (Web MVC, Data JPA, Validation).
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
