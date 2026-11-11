# Roadmap del backend — AgroDirecto

Cada tarea se desarrolla en su propia rama, se prueba y se integra a `main` con su commit.
La columna **Tema** relaciona la tarea con la Unidad 3 del curso.

| # | Rama | Tarea | Reglas de negocio | Tema del curso | Estado |
|---|------|-------|-------------------|----------------|--------|
| 0 | `main` (commit inicial) | Estructura base: Spring Boot, MySQL en Docker, esquema y datos demo con Flyway, manejo de errores | — | S17–18 Bases de datos, construcción de la BD | Completada |
| 1 | `feat/autenticacion-jwt` | Registro, login, Spring Security con roles y JWT | RN-07, RN-23 | S23–25 Autenticación, autorización, JWT | Completada |
| 2 | `feat/productores` | Perfil del productor, registro asistido por agente, verificación presencial, zonas de cobertura | RN-B1, RN-B5, RN-09 | S19–20 CRUD | Lista |
| 3 | `feat/catalogo` | Categorías, productos y formatos de venta, búsqueda, precios de referencia, alertas de precio bajo, favoritos | RN-02, RN-03, RN-A1, RN-A2, RN-A3 | S19–20 CRUD | Pendiente |
| 4 | `feat/pedidos` | Cotización, creación con validaciones, estados, cancelación, historial, datos de contacto protegidos | RN-01, RN-03, RN-09, RN-10, RN-11, RN-18, RN-24, RN-A4, RN-A5 | S19–20 CRUD | Pendiente |
| 5 | `feat/pagos-retenidos` | Pago simulado (Yape/Plin), retención, liberación al confirmar y liberación automática | RN-C1, RN-C2 | S19–20 CRUD | Pendiente |
| 6 | `feat/notificaciones-sms` | Mensajes al agricultor por WhatsApp (enlace wa.me) y respuesta por SMS (1 = aceptar, 2 = rechazar) | RN-B2 | — | Pendiente |
| 7 | `feat/tiempo-real` | WebSockets (STOMP): pedidos nuevos y cambios de estado en vivo | RN-05 | S21–22 WebSockets | Pendiente |
| 8 | `feat/confianza` | Reseñas verificadas, reputación del productor y disputas | RN-15, RN-20, RN-21, RN-22 | — | Pendiente |
| 9 | `feat/administracion` | Panel de métricas, verificación y suspensión de cuentas, resolución de disputas | RN-17, RN-23 | S24 Autorización | Pendiente |
| 10 | `feat/doble-factor-totp` | Segundo factor TOTP (Google Authenticator) para administrador y agente | — | S26 Seguridad en dos pasos | Pendiente |
| 11 | `docs/api-despliegue` | Documentación de la API (Swagger), Dockerfile y guía de despliegue (Aiven + Render/Railway) | — | S3 Plataformas de despliegue | Pendiente |

**Avance 2 (backend al 60%)**: tareas 0 a 6.
**Entrega final**: tareas 7 a 11 e integración con el frontend (repositorio `agrodirecto-project`).

## Flujo de trabajo por tarea

```bash
git switch main && git pull
git switch -c feat/<nombre>
# ...desarrollo y pruebas: ./mvnw test
git add -A && git commit -m "feat: ..."
git switch main && git merge feat/<nombre>
git push origin main feat/<nombre>
```
