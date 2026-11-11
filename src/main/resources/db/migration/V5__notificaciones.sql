-- Bandeja web (campana) y vínculo con el pedido que originó el mensaje.
ALTER TABLE notificacion ADD COLUMN leida BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notificacion ADD COLUMN pedido_id BIGINT;
CREATE INDEX idx_notificacion_usuario ON notificacion (usuario_id, leida);
CREATE INDEX idx_notificacion_productor ON notificacion (productor_id);
