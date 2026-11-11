-- Segundo factor TOTP: el secreto se guarda cifrado (AES-GCM) y se recuerda el último intervalo usado
-- para que un mismo código no pueda reutilizarse.
ALTER TABLE usuario ADD COLUMN totp_secreto VARCHAR(255);
ALTER TABLE usuario ADD COLUMN totp_activo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN totp_ultimo_paso BIGINT;

-- Códigos de respaldo de un solo uso (por si se pierde el celular). Solo se guarda su hash SHA-256.
CREATE TABLE codigo_respaldo (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    hash        VARCHAR(64) NOT NULL,
    usado       BOOLEAN     NOT NULL DEFAULT FALSE,
    usado_en    DATETIME,
    CONSTRAINT fk_respaldo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX idx_respaldo_usuario ON codigo_respaldo (usuario_id);
