-- Datos ficticios de demostración, alineados con el frontend (mismos 16 productos y 6 productores).
-- Contraseña de todas las cuentas demo: demo1234
-- Los precios de referencia son valores ilustrativos, no cotizaciones oficiales.

INSERT INTO usuario (nombre, email, password_hash, telefono, dni, ruc, razon_social, rol, verificado, estado, creado_en) VALUES
('Administración AgroDirecto', 'admin@agrodirecto.pe', '$2b$10$vHdqs0rOTghxr1wCnYoDnObPjIDr6g73LlAoP8JofTi9kqhTJjiZu', '900000001', NULL, NULL, NULL, 'ADMINISTRADOR', TRUE, 'ACTIVO', CURRENT_TIMESTAMP),
('Restaurante Sabor Andino', 'compras@saborandino.pe', '$2b$10$vHdqs0rOTghxr1wCnYoDnObPjIDr6g73LlAoP8JofTi9kqhTJjiZu', '900000002', '40000002', '20600000002', 'Sabor Andino S.A.C.', 'COMPRADOR', TRUE, 'ACTIVO', CURRENT_TIMESTAMP),
('Bodega Don Lucho', 'donlucho@correo.pe', '$2b$10$vHdqs0rOTghxr1wCnYoDnObPjIDr6g73LlAoP8JofTi9kqhTJjiZu', '900000003', '40000003', NULL, NULL, 'COMPRADOR', FALSE, 'ACTIVO', CURRENT_TIMESTAMP),
('Juan Huamán Rojas', 'andesverde@agrodirecto.pe', '$2b$10$vHdqs0rOTghxr1wCnYoDnObPjIDr6g73LlAoP8JofTi9kqhTJjiZu', '900000004', '40000004', NULL, NULL, 'PRODUCTOR', TRUE, 'ACTIVO', CURRENT_TIMESTAMP),
('Rosa Quispe Mamani', 'promotora@agrodirecto.pe', '$2b$10$vHdqs0rOTghxr1wCnYoDnObPjIDr6g73LlAoP8JofTi9kqhTJjiZu', '900000005', '40000005', NULL, NULL, 'AGENTE', TRUE, 'ACTIVO', CURRENT_TIMESTAMP);

INSERT INTO asociacion (nombre, ruc, region, emite_factura) VALUES
('Cooperativa Agraria Altura Puno', '20600000010', 'Puno', TRUE);

-- AndesVerde tiene cuenta propia; los demás agricultores no tienen computadora y los gestiona la promotora.
INSERT INTO productor (usuario_id, agente_id, asociacion_id, nombre_comercial, nombre_titular, dni, telefono, region, provincia, distrito, latitud, longitud, historia, practicas, tiene_ruc, ruc, metodo_cobro, numero_cobro, verificado, verificado_por, fecha_verificacion, estado, creado_en) VALUES
((SELECT id FROM usuario WHERE email = 'andesverde@agrodirecto.pe'), NULL, NULL, 'AndesVerde', 'Juan Huamán Rojas', '40000004', '900000004', 'Junín', 'Concepción', 'Concepción', -11.917000, -75.314000,
 'Familia productora de papa nativa en el valle del Mantaro desde hace tres generaciones.', 'Rotación de cultivos y abono orgánico.', FALSE, NULL, 'YAPE', '900000004', TRUE, (SELECT id FROM usuario WHERE email = 'admin@agrodirecto.pe'), CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP),
(NULL, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), NULL, 'ValleFresco', 'Pedro Ccahua Flores', '40000011', '900000011', 'Ica', 'Ica', 'Pachacútec', -14.040000, -75.728000,
 'Pequeño productor de hortalizas que antes vendía solo a acopiadores en chacra.', 'Riego por goteo.', FALSE, NULL, 'CUENTA_DNI', '40000011', TRUE, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP),
(NULL, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), NULL, 'AgroSol', 'María Yupanqui Huaraca', '40000012', '900000012', 'Ayacucho', 'Huanta', 'Huanta', -12.940000, -74.247000,
 'Productora de palta en la provincia de Huanta, organizada con vecinas de su comunidad.', 'Cosecha manual y selección por calibre.', FALSE, NULL, 'YAPE', '900000012', TRUE, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP),
(NULL, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), NULL, 'Finca El Mirador', 'Luis Santos Cárdenas', '40000013', '900000013', 'Pasco', 'Oxapampa', 'Villa Rica', -10.738000, -75.269000,
 'Caficultor de altura que tuesta en lotes pequeños.', 'Café bajo sombra.', TRUE, '10400000131', 'PLIN', '900000013', TRUE, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP),
(NULL, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), (SELECT id FROM asociacion WHERE ruc = '20600000010'), 'Altura Puno', 'Felipe Choque Apaza', '40000014', '900000014', 'Puno', 'Chucuito', 'Juli', -16.214000, -69.459000,
 'Socio de una cooperativa quinuera a orillas del Titicaca; la cooperativa emite la factura.', 'Quinua orgánica certificada por la cooperativa.', FALSE, NULL, 'CUENTA_DNI', '40000014', TRUE, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP),
(NULL, (SELECT id FROM usuario WHERE email = 'promotora@agrodirecto.pe'), NULL, 'Selva Norte', 'Ana Tapullima Sangama', '40000015', '900000015', 'San Martín', 'Moyobamba', 'Moyobamba', -6.034000, -76.974000,
 'Productora de plátano que corta por pedido para evitar mermas.', 'Corte por pedido.', FALSE, NULL, 'YAPE', '900000015', FALSE, NULL, NULL, 'ACTIVO', CURRENT_TIMESTAMP);

-- Zonas de cobertura: región propia y Lima; algunas zonas lejanas para demostrar RN-10.
INSERT INTO zona_cobertura (productor_id, region, dias_transito, pedido_minimo, costo_flete) VALUES
((SELECT id FROM productor WHERE dni = '40000004'), 'Junín', 0, 50.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000004'), 'Lima', 1, 150.00, 25.00),
((SELECT id FROM productor WHERE dni = '40000011'), 'Ica', 0, 50.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000011'), 'Lima', 1, 150.00, 25.00),
((SELECT id FROM productor WHERE dni = '40000011'), 'Cusco', 4, 400.00, 90.00),
((SELECT id FROM productor WHERE dni = '40000012'), 'Ayacucho', 0, 50.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000012'), 'Lima', 2, 200.00, 40.00),
((SELECT id FROM productor WHERE dni = '40000013'), 'Pasco', 0, 30.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000013'), 'Lima', 2, 100.00, 30.00),
((SELECT id FROM productor WHERE dni = '40000014'), 'Puno', 0, 50.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000014'), 'Lima', 3, 300.00, 60.00),
((SELECT id FROM productor WHERE dni = '40000015'), 'San Martín', 0, 30.00, 0.00),
((SELECT id FROM productor WHERE dni = '40000015'), 'Lima', 3, 300.00, 60.00);

INSERT INTO categoria (nombre) VALUES ('Tubérculos'), ('Hortalizas'), ('Frutas'), ('Granos');

INSERT INTO precio_referencia (clave, nombre, precio_mayorista_kg, precio_minorista_kg, fuente, fecha) VALUES
('PAPA_AMARILLA', 'Papa amarilla', 2.80, 4.50, 'Referencial de demostración', '2026-11-01'),
('PAPA_CANCHAN', 'Papa canchán', 1.60, 2.80, 'Referencial de demostración', '2026-11-01'),
('CAMOTE', 'Camote', 1.50, 3.00, 'Referencial de demostración', '2026-11-01'),
('TOMATE', 'Tomate', 1.80, 3.50, 'Referencial de demostración', '2026-11-01'),
('CEBOLLA_ROJA', 'Cebolla roja', 1.40, 2.80, 'Referencial de demostración', '2026-11-01'),
('PALTA_HASS', 'Palta Hass', 5.00, 9.00, 'Referencial de demostración', '2026-11-01'),
('PALTA_FUERTE', 'Palta fuerte', 4.00, 7.50, 'Referencial de demostración', '2026-11-01'),
('PLATANO', 'Plátano', 1.50, 3.00, 'Referencial de demostración', '2026-11-01'),
('QUINUA', 'Quinua', 7.00, 12.00, 'Referencial de demostración', '2026-11-01'),
('CAFE_TOSTADO', 'Café tostado', 30.00, 60.00, 'Referencial de demostración', '2026-11-01');

-- Productos en el mismo orden que el frontend (ids 1 a 16 en una base vacía).
INSERT INTO producto (productor_id, categoria_id, precio_referencia_id, nombre, descripcion, imagen_url, organico, fecha_cosecha, vida_util_dias, calibre, estado, creado_en, actualizado_en) VALUES
((SELECT id FROM productor WHERE dni = '40000004'), (SELECT id FROM categoria WHERE nombre = 'Tubérculos'), (SELECT id FROM precio_referencia WHERE clave = 'PAPA_AMARILLA'), 'Papa amarilla seleccionada', 'Papa amarilla de textura cremosa, seleccionada en origen y preparada para restaurantes y comercios.', '/images/products/potatoes.jpg', TRUE, '2026-08-12', 60, 'Primera', 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000011'), (SELECT id FROM categoria WHERE nombre = 'Hortalizas'), (SELECT id FROM precio_referencia WHERE clave = 'TOMATE'), 'Tomate italiano', 'Tomate firme y uniforme, ideal para cocina, salsas y abastecimiento de negocios gastronómicos.', '/images/products/tomatoes.jpg', FALSE, '2026-08-13', 10, 'Mediano', 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000012'), (SELECT id FROM categoria WHERE nombre = 'Frutas'), (SELECT id FROM precio_referencia WHERE clave = 'PALTA_HASS'), 'Palta Hass', 'Paltas calibradas y listas para maduración controlada, provenientes de pequeños productores asociados.', '/images/products/avocados.jpg', TRUE, '2026-08-10', 12, 'Calibre 16-18', 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000013'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'CAFE_TOSTADO'), 'Café orgánico tostado', 'Café arábica de altura con notas de cacao y frutos secos, tostado en lotes pequeños.', '/images/products/coffee.jpg', TRUE, '2026-08-11', 180, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000014'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'QUINUA'), 'Quinua blanca', 'Quinua limpia y seleccionada, con trazabilidad de comunidad y presentación para compra mayorista.', '/images/products/grains.jpg', TRUE, '2026-07-15', 365, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000015'), (SELECT id FROM categoria WHERE nombre = 'Frutas'), (SELECT id FROM precio_referencia WHERE clave = 'PLATANO'), 'Plátano isla', 'Plátano isla de pulpa dulce, cortado por pedido y acondicionado para transporte regional.', '/images/products/bananas.jpg', FALSE, '2026-08-12', 7, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000011'), (SELECT id FROM categoria WHERE nombre = 'Hortalizas'), (SELECT id FROM precio_referencia WHERE clave = 'CEBOLLA_ROJA'), 'Cebolla roja', 'Cebolla roja seca y clasificada por tamaño para comercios y cocinas de alta rotación.', '/images/products/tomatoes.jpg', FALSE, '2026-08-09', 45, 'Mediana', 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000004'), (SELECT id FROM categoria WHERE nombre = 'Tubérculos'), (SELECT id FROM precio_referencia WHERE clave = 'PAPA_CANCHAN'), 'Papa canchán', 'Variedad canchán de buen rendimiento, clasificada y empacada para abastecimiento comercial.', '/images/products/potatoes.jpg', FALSE, '2026-08-08', 60, 'Segunda', 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000011'), (SELECT id FROM categoria WHERE nombre = 'Hortalizas'), (SELECT id FROM precio_referencia WHERE clave = 'TOMATE'), 'Tomate cherry', 'Tomate cherry uniforme, fresco y listo para ensaladas, barras y restaurantes.', '/images/products/tomatoes.jpg', TRUE, '2026-08-13', 3, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000012'), (SELECT id FROM categoria WHERE nombre = 'Frutas'), (SELECT id FROM precio_referencia WHERE clave = 'PALTA_FUERTE'), 'Palta fuerte', 'Palta fuerte de productores familiares, con selección visual y control de maduración.', '/images/products/avocados.jpg', FALSE, '2026-08-10', 12, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000013'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'CAFE_TOSTADO'), 'Café tostado medio', 'Café de tueste medio para cafeterías y tiendas especializadas.', '/images/products/coffee.jpg', FALSE, '2026-08-12', 180, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000014'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'QUINUA'), 'Quinua roja', 'Quinua roja de altura con selección por lote y disponibilidad para compras recurrentes.', '/images/products/grains.jpg', TRUE, '2026-07-15', 365, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000004'), (SELECT id FROM categoria WHERE nombre = 'Tubérculos'), (SELECT id FROM precio_referencia WHERE clave = 'CAMOTE'), 'Camote amarillo', 'Camote amarillo seleccionado por tamaño, de pulpa dulce, para comercios y restaurantes.', '/images/products/potatoes.jpg', FALSE, '2026-08-14', 30, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000015'), (SELECT id FROM categoria WHERE nombre = 'Frutas'), (SELECT id FROM precio_referencia WHERE clave = 'PLATANO'), 'Plátano bellaco', 'Plátano bellaco firme, cosechado por pedido y acondicionado para transporte y preparación gastronómica.', '/images/products/bananas.jpg', FALSE, '2026-08-14', 7, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000014'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'QUINUA'), 'Quinua negra', 'Quinua negra de altura, seleccionada por lote y orientada a tiendas saludables.', '/images/products/grains.jpg', TRUE, '2026-07-15', 365, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM productor WHERE dni = '40000013'), (SELECT id FROM categoria WHERE nombre = 'Granos'), (SELECT id FROM precio_referencia WHERE clave = 'CAFE_TOSTADO'), 'Café molido de altura', 'Café arábica molido de tueste medio, preparado en lotes pequeños para cafeterías, tiendas y oficinas.', '/images/products/coffee.jpg', FALSE, '2026-08-14', 180, NULL, 'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Formatos de venta (RN-02). El formato del frontend se conserva y se agregan presentaciones menores.
INSERT INTO formato_venta (producto_id, nombre, tipo, peso_kg, precio, stock, cantidad_min_descuento, porcentaje_descuento, activo) VALUES
((SELECT id FROM producto WHERE nombre = 'Papa amarilla seleccionada'), 'Saco de 50 kg', 'MAYORISTA', 50.000, 118.00, 18, 10, 5.00, TRUE),
((SELECT id FROM producto WHERE nombre = 'Papa amarilla seleccionada'), 'Arroba de 12.5 kg', 'INTERMEDIO', 12.500, 32.00, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Papa amarilla seleccionada'), 'Bolsa de 5 kg', 'PEQUENO', 5.000, 14.00, 60, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Tomate italiano'), 'Caja de 20 kg', 'MAYORISTA', 20.000, 52.00, 26, 10, 5.00, TRUE),
((SELECT id FROM producto WHERE nombre = 'Tomate italiano'), 'Caja de 10 kg', 'INTERMEDIO', 10.000, 28.00, 30, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Tomate italiano'), 'Bolsa de 3 kg', 'PEQUENO', 3.000, 9.50, 50, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Palta Hass'), 'Jaba de 18 kg', 'MAYORISTA', 18.000, 96.00, 14, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Palta Hass'), 'Caja de 6 kg', 'INTERMEDIO', 6.000, 34.00, 20, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Palta Hass'), 'Bolsa de 2 kg', 'PEQUENO', 2.000, 12.50, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café orgánico tostado'), 'Caja de 5 kg', 'MAYORISTA', 5.000, 150.00, 8, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café orgánico tostado'), 'Bolsa de 1 kg', 'INTERMEDIO', 1.000, 32.00, 32, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café orgánico tostado'), 'Bolsa de 250 g', 'PEQUENO', 0.250, 9.50, 50, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua blanca'), 'Saco de 25 kg', 'MAYORISTA', 25.000, 145.00, 20, 5, 4.00, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua blanca'), 'Bolsa de 5 kg', 'INTERMEDIO', 5.000, 31.00, 30, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua blanca'), 'Bolsa de 1 kg', 'PEQUENO', 1.000, 7.00, 80, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Plátano isla'), 'Racimo', 'INTERMEDIO', NULL, 28.00, 24, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Plátano isla'), 'Mano', 'PEQUENO', NULL, 6.00, 60, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Cebolla roja'), 'Saco de 30 kg', 'MAYORISTA', 30.000, 82.00, 17, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Cebolla roja'), 'Bolsa de 5 kg', 'PEQUENO', 5.000, 15.00, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Papa canchán'), 'Saco de 50 kg', 'MAYORISTA', 50.000, 95.00, 12, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Papa canchán'), 'Arroba de 12.5 kg', 'INTERMEDIO', 12.500, 25.00, 30, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Tomate cherry'), 'Caja de 10 kg', 'MAYORISTA', 10.000, 44.00, 19, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Tomate cherry'), 'Bandeja de 1 kg', 'PEQUENO', 1.000, 6.00, 50, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Palta fuerte'), 'Jaba de 18 kg', 'MAYORISTA', 18.000, 84.00, 9, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Palta fuerte'), 'Bolsa de 2 kg', 'PEQUENO', 2.000, 11.00, 30, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café tostado medio'), 'Bolsa de 1 kg', 'INTERMEDIO', 1.000, 34.00, 20, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café tostado medio'), 'Bolsa de 500 g', 'PEQUENO', 0.500, 18.00, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua roja'), 'Saco de 25 kg', 'MAYORISTA', 25.000, 168.00, 11, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua roja'), 'Bolsa de 1 kg', 'PEQUENO', 1.000, 8.00, 50, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Camote amarillo'), 'Saco de 25 kg', 'MAYORISTA', 25.000, 64.00, 15, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Camote amarillo'), 'Bolsa de 5 kg', 'PEQUENO', 5.000, 14.00, 30, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Plátano bellaco'), 'Racimo', 'INTERMEDIO', NULL, 31.00, 21, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Plátano bellaco'), 'Mano', 'PEQUENO', NULL, 7.00, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua negra'), 'Saco de 25 kg', 'MAYORISTA', 25.000, 176.00, 8, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Quinua negra'), 'Bolsa de 1 kg', 'PEQUENO', 1.000, 8.50, 40, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café molido de altura'), 'Bolsa de 1 kg', 'INTERMEDIO', 1.000, 40.00, 20, NULL, NULL, TRUE),
((SELECT id FROM producto WHERE nombre = 'Café molido de altura'), 'Bolsa de 500 g', 'PEQUENO', 0.500, 21.00, 35, NULL, NULL, TRUE);
