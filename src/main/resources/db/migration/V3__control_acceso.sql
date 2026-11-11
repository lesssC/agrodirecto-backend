-- Bloqueo temporal ante intentos fallidos de inicio de sesión (protección contra fuerza bruta).
ALTER TABLE usuario ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN bloqueado_hasta DATETIME;
ALTER TABLE usuario ADD COLUMN ultimo_acceso DATETIME;

-- Con JWT la sesión vive en el token; la tabla de sesiones del esquema inicial ya no se usa.
DROP TABLE sesion;
