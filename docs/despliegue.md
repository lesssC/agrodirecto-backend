# Guía de despliegue

AgroDirecto está publicado así (noviembre de 2026):

| Pieza | Servicio | Plan | Dirección |
|---|---|---|---|
| Frontend (`agrodirecto-project`) | Cloudflare Workers | Free | https://agrodirecto-avance-1.agrodirecto-fullstack.workers.dev |
| API (este repositorio) | Render, con el `Dockerfile` | Free (512 MB) | https://agrodirecto-api.onrender.com |
| Documentación de la API | Swagger UI en la API | — | https://agrodirecto-api.onrender.com/swagger-ui.html |
| Base de datos MySQL 8.4 | Azure Database for MySQL – Servidor flexible | Burstable B1ms, 20 GiB (Azure for Students) | `agrodirecto-db-df.mysql.database.azure.com`, región Mexico Central |

Orden de despliegue: **1. base de datos → 2. API → 3. frontend**, porque cada pieza necesita la dirección de la anterior.

## 1. Base de datos en Azure (Azure for Students)

Azure for Students no pide tarjeta: da un crédito de USD 100 y, durante 12 meses, 750 horas al mes de MySQL B1ms y 32 GiB de almacenamiento gratis. Si el crédito se acaba, Azure detiene los recursos; no cobra.

1. En https://portal.azure.com buscar **Azure Database for MySQL Flexible Server** (opción de *Marketplace*) y elegir **Creación avanzada**.
2. **Datos básicos**:

| Campo | Valor |
|---|---|
| Suscripción | Azure for Students |
| Grupo de recursos | `agrodirecto` (crear nuevo) |
| Nombre del servidor | `agrodirecto-db-df` |
| Región | **Mexico Central** (ver la nota de regiones) |
| Versión de MySQL | 8.4 |
| Tipo de carga de trabajo | **Desarrollo/pruebas** |
| Proceso y almacenamiento | *Configurar servidor* → **Burstable, Standard_B1ms**, **20 GiB**, crecimiento automático desactivado |
| Alta disponibilidad | **Deshabilitado** |
| Autenticación | Autenticación de MySQL; usuario `agroadmin` y una contraseña fuerte |

3. **Redes**: *Acceso público*, marcar "Permitir el acceso público desde cualquier servicio de Azure" y agregar la regla `render` de `0.0.0.0` a `255.255.255.255` (el plan gratuito de Render no tiene una IP fija; la base queda protegida por la contraseña y la conexión cifrada).
4. En **Revisar y crear** comprobar que diga **B1ms** y **20 GiB** antes de pulsar *Crear*. El cuadro "Estimated costs" a veces no se actualiza y muestra un tamaño grande (D4ads, más de USD 200 al mes): lo que vale es la pantalla de revisión. El precio de lista de B1ms (unos USD 12 al mes) queda cubierto por el beneficio gratuito.
5. Al terminar (5–10 minutos): **Configuración → Bases de datos → + Agregar** → `agrodirecto`.

URL JDBC para la API:

```
jdbc:mysql://agrodirecto-db-df.mysql.database.azure.com:3306/agrodirecto?sslMode=REQUIRED&serverTimezone=America/Lima
```

Flyway crea las tablas y carga los datos de demostración en el primer arranque de la API.

### Nota sobre regiones

La suscripción Azure for Students de la UTP solo permite `northcentralus`, `westus`, `canadacentral`, `chilecentral` y `mexicocentral` (se ve en **Directiva → Tareas**, o en el detalle del error "policy violation"). Aun así, **West US** rechaza MySQL para estudiantes ("no se puede aprovisionar"). Mexico Central y Chile Central sí funcionan; se eligió Mexico Central por estar más cerca de Render (Oregon).

### Costos

- Revisar **Cost Management → Análisis de costos** un día después de crear el servidor: debe estar en USD 0 o unos centavos.
- Entre exposiciones se puede pulsar **Detener** en el servidor (Azure lo vuelve a encender solo a los 30 días).
- Al terminar el curso: **Grupos de recursos → agrodirecto → Eliminar grupo de recursos** borra todo.

## 2. API en Render

1. Entrar a https://dashboard.render.com con la cuenta de GitHub dueña de este repositorio.
2. **New → Blueprint** → elegir `agrodirecto-backend`. Render lee `render.yaml` y crea el servicio `agrodirecto-api` (Docker, plan Free, región Oregon).
3. Completar las variables que el blueprint deja vacías:

| Variable | Valor |
|---|---|
| `DB_URL` | La URL JDBC del paso 1 |
| `DB_USER` | `agroadmin` (en el servidor flexible va sin `@servidor`) |
| `DB_PASSWORD` | La contraseña del servidor |
| `ORIGENES_PERMITIDOS` | `https://agrodirecto-avance-1.agrodirecto-fullstack.workers.dev` (al inicio se puede usar `https://*.workers.dev`) |

`JWT_SECRET`, `TOTP_CLAVE` y `SMS_WEBHOOK_SECRET` los genera Render al azar. **No cambiar `TOTP_CLAVE` después**: cifra los secretos del segundo factor guardados en la base.

4. La primera compilación tarda unos 3–10 minutos. En **Logs** deben aparecer `Successfully applied 8 migrations` y `Started AgrodirectoBackendApplication`; luego el estado pasa a **Live**.
5. Comprobar:

```bash
curl https://agrodirecto-api.onrender.com/actuator/health   # {"status":"UP"}
```

Cada push a `main` vuelve a desplegar la API automáticamente.

**Errores frecuentes**

| En los logs | Causa |
|---|---|
| `Communications link failure` | Falta la regla de firewall en Azure, o el host de `DB_URL` está mal |
| `Access denied for user` | Usuario o contraseña incorrectos |
| `Unknown database 'agrodirecto'` | Falta crear la base en Azure; crearla y usar *Manual Deploy → Deploy latest commit* |

### Límites del plan gratuito

- El servicio se duerme tras 15 minutos sin visitas; la primera petición después tarda cerca de un minuto. **Antes de una exposición**, abrir `/actuator/health` unos minutos antes.
- 512 MB de memoria: por eso `JAVA_OPTS` (en `render.yaml`) usa el recolector serial y limita el compilador.

## 3. Frontend en Cloudflare Workers

Se publica desde la carpeta de `agrodirecto-project` con Wrangler. En WSL, `wrangler login` no puede abrir el navegador, así que se usa un token:

1. En Cloudflare: **My Profile → API Tokens → Create Token → plantilla "Edit Cloudflare Workers"**.
2. Guardarlo en un archivo (no en el repositorio):

```bash
echo 'TOKEN' > ~/.cf-token && chmod 600 ~/.cf-token
```

3. Compilar con la dirección de la API y publicar:

```bash
cd agrodirecto-project
VITE_API_URL=https://agrodirecto-api.onrender.com pnpm build
CLOUDFLARE_API_TOKEN=$(cat ~/.cf-token) pnpm exec wrangler deploy
```

`VITE_API_URL` se fija al compilar: si cambia la dirección de la API, hay que volver a compilar y publicar. El WebSocket usa `wss://` automáticamente porque la API está en `https://`.

Al terminar el curso, revocar el token en Cloudflare y borrar `~/.cf-token`.

## Demostración frente a producción real

Este despliegue es para la demostración del curso: carga las cuentas demo (contraseña `demo1234`) y deja activos sus códigos TOTP conocidos. Para un uso real:

| Cambio | Cómo |
|---|---|
| No cargar los datos demo | `SPRING_FLYWAY_LOCATIONS=classpath:db/migration` en la **primera** puesta en marcha |
| Desactivar los secretos TOTP conocidos | `DOBLE_FACTOR_DEMO=false` |
| Ocultar Swagger | `SWAGGER=false` |
| Restringir el firewall | Usar un servicio con IP fija y permitir solo esa IP en Azure |
| Alternativas sin Azure | Cualquier MySQL 8 con TLS sirve (por ejemplo TiDB Cloud Starter, probado localmente con TiDB 8.5); Aiven ya no ofrece plan gratuito |
