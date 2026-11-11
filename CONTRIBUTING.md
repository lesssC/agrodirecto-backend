# Guía para el equipo

Cómo clonar AgroDirecto, levantarlo en tu computadora y subir cambios. El proyecto tiene dos repositorios:

| Repositorio | Contenido |
|---|---|
| [lesssC/agrodirecto-backend](https://github.com/lesssC/agrodirecto-backend) | API REST (Spring Boot + MySQL) |
| [andrebm99/agrodirecto-project](https://github.com/andrebm99/agrodirecto-project) | Aplicación web (React) |

## 0. Antes de empezar (una sola vez)

**Acceso:** para subir cambios necesitas ser colaborador de cada repositorio. Pide que te agreguen (en GitHub: *Settings → Collaborators → Add people*) y acepta la invitación que llega por correo.

**Instala:**

- **Git**
- **Docker Desktop**: levanta la API y MySQL; no necesitas instalar Java ni MySQL.
- **Node.js 22.13 o superior**
- **pnpm**: después de instalar Node, ejecuta `corepack enable`.

En Windows, usa **WSL** o **Git Bash**: los comandos de esta guía son de bash.

## 1. Clonar los dos repositorios

Conviene tenerlos en la misma carpeta:

```bash
mkdir agrodirecto && cd agrodirecto
git clone https://github.com/lesssC/agrodirecto-backend.git
git clone https://github.com/andrebm99/agrodirecto-project.git
```

## 2. Levantar el proyecto localmente

```bash
# Backend: API + MySQL con datos de demostración
cd agrodirecto-backend
docker compose up -d --build          # la primera vez tarda unos minutos
curl localhost:8080/actuator/health   # debe responder {"status":"UP"}
# Documentación de la API: http://localhost:8080/swagger-ui.html

# Frontend (en otra terminal)
cd agrodirecto-project
cp .env.example .env.local            # solo la primera vez
pnpm install
pnpm dev                              # abre http://localhost:3000
```

- **Cuentas demo:** todas usan la contraseña `demo1234`. En el login, "Cuentas de demostración" las completa con un clic. Las cuentas de agente y administración piden además un código TOTP (las claves están en el README del frontend).
- **Detener:** `Ctrl+C` en el frontend y `docker compose down` en el backend.
- **Volver a los datos iniciales:** `docker compose down -v` y luego `docker compose up -d --build`.

## 3. Subir cambios: una rama por tarea

```bash
git checkout main && git pull                 # partir siempre de lo último
git checkout -b feat/nombre-de-la-tarea       # rama nueva para la tarea
# ... hacer los cambios ...
```

Antes de subir, verifica que todo pase:

| Repositorio | Comando |
|---|---|
| Frontend | `pnpm typecheck && pnpm lint && pnpm format:check && pnpm test` |
| Backend | `./mvnw test` (necesita Java 21). Sin Java: `docker compose up -d --build` y probar la API |

```bash
git add .
git commit -m "feat: descripción corta del cambio"
git push -u origin feat/nombre-de-la-tarea
```

Luego, en GitHub, pulsa **Compare & pull request**, crea el *Pull Request* hacia `main`, pide que otro compañero lo revise y recién después haz el **Merge**.

**Prefijos de rama y commit:** `feat/` (función nueva), `fix/` (corrección), `docs/` (documentación), `refactor/` (reorganizar sin cambiar el comportamiento).

### No subas directo a `main`

- **Backend:** cada cambio que entra a `main` **se publica solo en Render** (producción). Si algo está roto, se cae la API de la demostración.
- **Frontend:** la versión publicada **no se actualiza sola**; la publica quien tiene el token de Cloudflare (ver abajo).

## 4. Publicar (solo quien administra el despliegue)

- **API:** automática al hacer merge a `main` del backend. Se puede seguir en el panel de Render (*Logs*).
- **Frontend:** desde `agrodirecto-project`, con el token de Cloudflare guardado en `~/.cf-token`:

```bash
VITE_API_URL=https://agrodirecto-api.onrender.com pnpm build
CLOUDFLARE_API_TOKEN=$(cat ~/.cf-token) pnpm exec wrangler deploy
```

La guía completa del despliegue (Azure MySQL, Render y Cloudflare) está en [docs/despliegue.md](https://github.com/lesssC/agrodirecto-backend/blob/main/docs/despliegue.md).

## Direcciones públicas

- Aplicación: https://agrodirecto-avance-1.agrodirecto-fullstack.workers.dev
- API: https://agrodirecto-api.onrender.com (Swagger en `/swagger-ui.html`)

El plan gratuito de Render duerme la API tras 15 minutos sin uso: antes de una exposición, abre https://agrodirecto-api.onrender.com/actuator/health unos minutos antes.
