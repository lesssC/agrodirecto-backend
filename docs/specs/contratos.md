# Contratos backend ↔ frontend (acordar el mié 25)

Errores: formato `ApiError` existente (`estado`, `mensaje`, `regla`, `detalles`). Auth: `Authorization: Bearer <JWT>`. Cambiar este archivo solo por PR aparte.

## Archivos (P2-01)
`POST /api/archivos` · `multipart/form-data` · autenticado
- Campos: `archivo` (file), `proposito` ∈ `PRODUCTO | DISPUTA | DESCARGO`.
- Límite 5 MB; tipos `image/jpeg`, `image/png`, `image/webp` (validar contenido real).
- `201 {"url": "https://…", "tipo": "image/webp", "tamanoBytes": 123456}`
- Errores: 400 `ARCHIVO_TIPO_INVALIDO`, 413 `ARCHIVO_DEMASIADO_GRANDE`, 401 sin sesión.

## Pago (P1-03)
`POST /api/pedidos/{id}/pago` (comprador del pedido). Body actual (`metodo`, `numeroOperacion?`) sin cambios.
- Respuesta: `{"pagoId": 1, "estado": "RETENIDO | PENDIENTE", "urlCheckout": "https://…" | null}`.
- Modo simulado (`pagos.proveedor=simulado`): `urlCheckout=null`, `estado=RETENIDO` (como hoy).
- Modo sandbox: `estado=PENDIENTE` y `urlCheckout` con la URL de pago; solo el webhook pasa a `RETENIDO`.
`POST /api/pagos/webhook` · público · firma HMAC en cabecera `X-Firma` · idempotente por `idExterno` · responde 200 siempre que la firma sea válida, 401 si no.
El frontend se entera del cambio por STOMP `/user/queue/pedidos` (ya existente) o refrescando `GET /api/pedidos/{id}`.

## Recuperación de contraseña (P2-02)
- `POST /api/auth/olvide-contrasena {"email"}` → `202` siempre (exista o no).
- `POST /api/auth/restablecer {"token","nuevaContrasena"}` → `204`; 400 `TOKEN_INVALIDO` (expirado, usado o inexistente); 400 de validación si la contraseña es débil.
- El correo contiene el enlace `${FRONTEND_URL}/#restablecer?token=<token>`.

## Categorías admin (P2-03)
`GET /api/admin/categorias`, `POST /api/admin/categorias {"nombre"}`, `PUT /api/admin/categorias/{id}`, `DELETE /api/admin/categorias/{id}` (409 `CATEGORIA_EN_USO` si tiene productos). Solo `ADMINISTRADOR`.

## Admin pedidos/pagos (P1-04)
`GET /api/admin/pedidos?estado=&pagina=0&tamano=20` y `GET /api/admin/pagos?estado=&pagina=&tamano=` → `{"contenido":[…],"pagina":0,"tamano":20,"total":N}`.
`POST /api/admin/pagos/{id}/marcar-transferido` → 204 (registra que se pagó al agricultor fuera del sistema).
