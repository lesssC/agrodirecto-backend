-- Registro inalterable de las acciones de la administración: quién, sobre qué, cuándo y por qué.
CREATE TABLE accion_administrativa (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    administrador_id BIGINT       NOT NULL,
    tipo           VARCHAR(40)  NOT NULL,
    objetivo       VARCHAR(30)  NOT NULL, -- USUARIO, PRODUCTOR, PRECIO_REFERENCIA
    objetivo_id    BIGINT       NOT NULL,
    detalle        VARCHAR(500),
    creado_en      DATETIME     NOT NULL,
    CONSTRAINT fk_accion_admin FOREIGN KEY (administrador_id) REFERENCES usuario (id)
);
CREATE INDEX idx_accion_objetivo ON accion_administrativa (objetivo, objetivo_id);
