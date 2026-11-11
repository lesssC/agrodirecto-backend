-- AgroDirecto: esquema inicial (MySQL 8).
-- Los estados y tipos se guardan como VARCHAR para que el mismo script funcione en las pruebas con H2.

CREATE TABLE usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    telefono       VARCHAR(20),
    dni            VARCHAR(8),
    ruc            VARCHAR(11),
    razon_social   VARCHAR(160),
    rol            VARCHAR(20)  NOT NULL,          -- COMPRADOR, PRODUCTOR, AGENTE, ADMINISTRADOR
    verificado     BOOLEAN      NOT NULL DEFAULT FALSE,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO', -- ACTIVO, SUSPENDIDO
    creado_en      DATETIME     NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_telefono UNIQUE (telefono),
    CONSTRAINT uk_usuario_dni UNIQUE (dni)
);

CREATE TABLE sesion (
    token       VARCHAR(64) PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    expira_en   DATETIME    NOT NULL,
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- Cooperativas o asociaciones: agrupan oferta y pueden emitir factura por el agricultor sin RUC.
CREATE TABLE asociacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(160) NOT NULL,
    ruc            VARCHAR(11),
    region         VARCHAR(60)  NOT NULL,
    emite_factura  BOOLEAN      NOT NULL DEFAULT FALSE
);

-- El agricultor puede no tener cuenta propia: en ese caso un agente gestiona su perfil.
CREATE TABLE productor (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id          BIGINT,
    agente_id           BIGINT,
    asociacion_id       BIGINT,
    nombre_comercial    VARCHAR(120) NOT NULL,
    nombre_titular      VARCHAR(120) NOT NULL,
    dni                 VARCHAR(8)   NOT NULL,
    telefono            VARCHAR(20)  NOT NULL,
    region              VARCHAR(60)  NOT NULL,
    provincia           VARCHAR(60)  NOT NULL,
    distrito            VARCHAR(60),
    latitud             DECIMAL(9, 6),
    longitud            DECIMAL(9, 6),
    historia            VARCHAR(1000),
    practicas           VARCHAR(500),
    tiene_ruc           BOOLEAN      NOT NULL DEFAULT FALSE,
    ruc                 VARCHAR(11),
    metodo_cobro        VARCHAR(20)  NOT NULL,     -- YAPE, PLIN, CUENTA_DNI, CUENTA_BANCARIA
    numero_cobro        VARCHAR(30)  NOT NULL,
    verificado          BOOLEAN      NOT NULL DEFAULT FALSE,
    verificado_por      BIGINT,
    fecha_verificacion  DATETIME,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    creado_en           DATETIME     NOT NULL,
    CONSTRAINT uk_productor_dni UNIQUE (dni),
    CONSTRAINT uk_productor_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_productor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_productor_agente FOREIGN KEY (agente_id) REFERENCES usuario (id),
    CONSTRAINT fk_productor_asociacion FOREIGN KEY (asociacion_id) REFERENCES asociacion (id),
    CONSTRAINT fk_productor_verificador FOREIGN KEY (verificado_por) REFERENCES usuario (id)
);

-- Regiones a las que el productor puede abastecer (RN-09, RN-10, RN-11).
CREATE TABLE zona_cobertura (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    productor_id   BIGINT        NOT NULL,
    region         VARCHAR(60)   NOT NULL,
    dias_transito  INT           NOT NULL,
    pedido_minimo  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    costo_flete    DECIMAL(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_zona UNIQUE (productor_id, region),
    CONSTRAINT fk_zona_productor FOREIGN KEY (productor_id) REFERENCES productor (id)
);

CREATE TABLE categoria (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(60) NOT NULL,
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

-- Precios de mercado por kg para proteger al agricultor de malvender (RN-A2, RN-A3).
CREATE TABLE precio_referencia (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    clave                VARCHAR(60)    NOT NULL,
    nombre               VARCHAR(120)   NOT NULL,
    precio_mayorista_kg  DECIMAL(10, 2) NOT NULL,
    precio_minorista_kg  DECIMAL(10, 2) NOT NULL,
    fuente               VARCHAR(160)   NOT NULL,
    fecha                DATE           NOT NULL,
    CONSTRAINT uk_precio_clave UNIQUE (clave)
);

CREATE TABLE producto (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    productor_id          BIGINT        NOT NULL,
    categoria_id          BIGINT        NOT NULL,
    precio_referencia_id  BIGINT,
    nombre                VARCHAR(120)  NOT NULL,
    descripcion           VARCHAR(1000),
    imagen_url            VARCHAR(500),
    organico              BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_cosecha         DATE,
    vida_util_dias        INT,
    calibre               VARCHAR(60),
    estado                VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR', -- ACTIVO, BORRADOR, RETIRADO
    creado_en             DATETIME      NOT NULL,
    actualizado_en        DATETIME      NOT NULL,
    CONSTRAINT fk_producto_productor FOREIGN KEY (productor_id) REFERENCES productor (id),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_producto_precio_ref FOREIGN KEY (precio_referencia_id) REFERENCES precio_referencia (id)
);

-- Presentaciones de venta (RN-02): pequeña, intermedia y mayorista, cada una con precio y stock.
CREATE TABLE formato_venta (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id             BIGINT         NOT NULL,
    nombre                  VARCHAR(80)    NOT NULL,
    tipo                    VARCHAR(20)    NOT NULL, -- PEQUENO, INTERMEDIO, MAYORISTA
    peso_kg                 DECIMAL(10, 3),
    precio                  DECIMAL(10, 2) NOT NULL,
    stock                   INT            NOT NULL,
    cantidad_min_descuento  INT,
    porcentaje_descuento    DECIMAL(5, 2),
    activo                  BOOLEAN        NOT NULL DEFAULT TRUE,
    version                 BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_formato_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

CREATE TABLE pedido (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo                  VARCHAR(20),
    comprador_id            BIGINT         NOT NULL,
    productor_id            BIGINT         NOT NULL,
    estado                  VARCHAR(30)    NOT NULL,
    modalidad_entrega       VARCHAR(30)    NOT NULL,
    region_destino          VARCHAR(60)    NOT NULL,
    direccion_entrega       VARCHAR(250)   NOT NULL,
    nota                    VARCHAR(500),
    subtotal                DECIMAL(12, 2) NOT NULL,
    descuento               DECIMAL(12, 2) NOT NULL,
    costo_flete             DECIMAL(12, 2) NOT NULL,
    comision                DECIMAL(12, 2) NOT NULL,
    total                   DECIMAL(12, 2) NOT NULL,
    monto_productor         DECIMAL(12, 2) NOT NULL,
    fecha_entrega_estimada  DATE,
    entregado_en            DATETIME,
    creado_en               DATETIME       NOT NULL,
    actualizado_en          DATETIME       NOT NULL,
    CONSTRAINT uk_pedido_codigo UNIQUE (codigo),
    CONSTRAINT fk_pedido_comprador FOREIGN KEY (comprador_id) REFERENCES usuario (id),
    CONSTRAINT fk_pedido_productor FOREIGN KEY (productor_id) REFERENCES productor (id)
);

CREATE TABLE detalle_pedido (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id             BIGINT         NOT NULL,
    formato_id            BIGINT         NOT NULL,
    cantidad              INT            NOT NULL,
    precio_unitario       DECIMAL(10, 2) NOT NULL,
    porcentaje_descuento  DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    subtotal              DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_detalle_formato FOREIGN KEY (formato_id) REFERENCES formato_venta (id)
);

-- Pago simulado con retención (RN-C1): el comprador paga primero y se libera al agricultor tras la entrega.
CREATE TABLE pago (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id    BIGINT         NOT NULL,
    monto        DECIMAL(12, 2) NOT NULL,
    estado       VARCHAR(20)    NOT NULL, -- PENDIENTE, RETENIDO, LIBERADO, DEVUELTO
    metodo       VARCHAR(20),
    referencia   VARCHAR(60),
    pagado_en    DATETIME,
    liberado_en  DATETIME,
    devuelto_en  DATETIME,
    CONSTRAINT uk_pago_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
);

-- Trazabilidad de cada cambio de estado (antifraude y auditoría).
CREATE TABLE historial_pedido (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id        BIGINT       NOT NULL,
    estado_anterior  VARCHAR(30),
    estado_nuevo     VARCHAR(30)  NOT NULL,
    usuario_id       BIGINT,
    canal            VARCHAR(20)  NOT NULL, -- WEB, SMS, SISTEMA
    comentario       VARCHAR(300),
    creado_en        DATETIME     NOT NULL,
    CONSTRAINT fk_historial_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE resena (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id     BIGINT       NOT NULL,
    productor_id  BIGINT       NOT NULL,
    autor_id      BIGINT       NOT NULL,
    calificacion  INT          NOT NULL,
    comentario    VARCHAR(500),
    creado_en     DATETIME     NOT NULL,
    CONSTRAINT uk_resena_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_resena_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_resena_productor FOREIGN KEY (productor_id) REFERENCES productor (id),
    CONSTRAINT fk_resena_autor FOREIGN KEY (autor_id) REFERENCES usuario (id)
);

CREATE TABLE disputa (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL,
    abierta_por    BIGINT        NOT NULL,
    motivo         VARCHAR(40)   NOT NULL, -- NO_ENTREGADO, CALIDAD, CANTIDAD, PRODUCTO_DISTINTO, OTRO
    descripcion    VARCHAR(1000) NOT NULL,
    evidencia_url  VARCHAR(500),
    estado         VARCHAR(30)   NOT NULL, -- ABIERTA, A_FAVOR_COMPRADOR, A_FAVOR_PRODUCTOR
    resolucion     VARCHAR(1000),
    resuelta_por   BIGINT,
    creada_en      DATETIME      NOT NULL,
    resuelta_en    DATETIME,
    CONSTRAINT uk_disputa_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_disputa_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_disputa_abierta_por FOREIGN KEY (abierta_por) REFERENCES usuario (id),
    CONSTRAINT fk_disputa_resuelta_por FOREIGN KEY (resuelta_por) REFERENCES usuario (id)
);

CREATE TABLE favorito (
    usuario_id   BIGINT NOT NULL,
    producto_id  BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, producto_id),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_favorito_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

-- Mensajes al agricultor por WhatsApp/SMS (RN-B2). En el prototipo se registran y se genera el enlace wa.me.
CREATE TABLE notificacion (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    productor_id  BIGINT,
    usuario_id    BIGINT,
    canal         VARCHAR(20)  NOT NULL, -- WHATSAPP, SMS, WEB
    destino       VARCHAR(20),
    mensaje       VARCHAR(500) NOT NULL,
    enlace        VARCHAR(700),
    creada_en     DATETIME     NOT NULL,
    CONSTRAINT fk_notificacion_productor FOREIGN KEY (productor_id) REFERENCES productor (id),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_producto_estado ON producto (estado);
CREATE INDEX idx_pedido_comprador ON pedido (comprador_id);
CREATE INDEX idx_pedido_productor ON pedido (productor_id);
CREATE INDEX idx_pedido_estado ON pedido (estado);
