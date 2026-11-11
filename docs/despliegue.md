# Guía de despliegue (sin costo)

| Pieza | Servicio | Plan |
|---|---|---|
| Base de datos MySQL 8 | [Aiven](https://aiven.io) | Free (1 GB) |
| API (este repositorio) | [Render](https://render.com) con el `Dockerfile` | Free (512 MB) |
| Frontend (`agrodirecto-project`) | Cloudflare Workers | Free |

Alternativa a Render: [Railway](https://railway.com) también construye el `Dockerfile` y usa las mismas variables (su plan gratuito es un crédito mensual limitado).

## 1. Base de datos en Aiven

1. Crear una cuenta y un servicio **MySQL**, plan **Free**, en la región más cercana disponible.
2. En *Overview → Connection information* copiar `Host`, `Port`, `User` (`avnadmin`) y `Password`.
3. La base `defaultdb` ya existe; Flyway crea las tablas y carga los datos de demostración en el primer arranque.
4. Aiven exige conexión cifrada. La URL JDBC queda así:

```
jdbc:mysql://HOST:PUERTO/defaultdb?sslMode=REQUIRED&serverTimezone=America/Lima
```

Aiven activa `sql_require_primary_key`: todas las tablas de las migraciones tienen clave primaria, así que no hay que cambiar nada.

## 2. API en Render

1. Subir este repositorio a GitHub (ya está en `main`).
2. En Render: **New → Blueprint** y elegir el repositorio. Render lee `render.yaml` y crea el servicio `agrodirecto-api` con el `Dockerfile`.
3. Completar las variables que el blueprint deja vacías:

| Variable | Valor |
|---|---|
| `DB_URL` | La URL JDBC del paso 1 |
| `DB_USER` / `DB_PASSWORD` | Usuario y contraseña de Aiven |
| `ORIGENES_PERMITIDOS` | Dominio del frontend, por ejemplo `https://agrodirecto.tu-cuenta.workers.dev` (varios, separados por comas) |

`JWT_SECRET`, `TOTP_CLAVE` y `SMS_WEBHOOK_SECRET` los genera Render al azar. **No cambiar `TOTP_CLAVE` después**: cifra los secretos del segundo factor guardados en la base; si cambia, esos secretos ya no se pueden leer.

4. Esperar el primer despliegue (unos 5 minutos) y comprobar:

```bash
curl https://agrodirecto-api.onrender.com/actuator/health   # {"status":"UP"}
```

La documentación interactiva queda en `https://agrodirecto-api.onrender.com/swagger-ui.html`.

**Sin blueprint:** *New → Web Service*, runtime **Docker**, plan **Free**, *Health Check Path* `/actuator/health` y las mismas variables. Render define `PORT` y la API lo usa.

### Límites del plan gratuito

- El servicio se duerme tras 15 minutos sin visitas; la primera petición después tarda cerca de un minuto. Antes de una exposición, abrir `/actuator/health` unos minutos antes.
- 512 MB de memoria: por eso `JAVA_OPTS` usa el recolector serial y limita el compilador. Si el servicio se reinicia por memoria, revisar los logs en Render.
- El WebSocket (`/ws`) funciona en Render; el frontend usa `wss://` automáticamente cuando la API está en `https://`.

## 3. Frontend en Cloudflare

En el proyecto del frontend, definir `VITE_API_URL=https://agrodirecto-api.onrender.com` antes de compilar y desplegar (ver el README de `agrodirecto-project`). Luego agregar ese dominio en `ORIGENES_PERMITIDOS` de Render.

## Demostración frente a producción real

Este despliegue es para la demostración del curso: carga las cuentas demo y deja activos sus códigos TOTP conocidos. Para un uso real:

| Cambio | Cómo |
|---|---|
| No cargar los datos demo | `SPRING_FLYWAY_LOCATIONS=classpath:db/migration` en la **primera** puesta en marcha |
| Desactivar los secretos TOTP conocidos | `DOBLE_FACTOR_DEMO=false` |
| Ocultar Swagger | `SWAGGER=false` |
| Respaldos | Aiven Free no incluye respaldos automáticos útiles; usar un plan pago o `mysqldump` programado |
