-- Al liberar el pago se registra cuánto recibió el agricultor, cuánto quedó como servicio y a qué cuenta se envió.
ALTER TABLE pago ADD COLUMN monto_productor DECIMAL(12, 2);
ALTER TABLE pago ADD COLUMN comision DECIMAL(12, 2);
ALTER TABLE pago ADD COLUMN destino_metodo VARCHAR(20);
ALTER TABLE pago ADD COLUMN destino_numero VARCHAR(30);
ALTER TABLE pago ADD COLUMN creado_en DATETIME;
