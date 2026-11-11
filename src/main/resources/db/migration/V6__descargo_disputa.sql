-- El agricultor también tiene voz en la disputa: su descargo y su propia evidencia.
ALTER TABLE disputa ADD COLUMN descargo VARCHAR(1000);
ALTER TABLE disputa ADD COLUMN evidencia_descargo_url VARCHAR(500);
ALTER TABLE disputa ADD COLUMN descargo_en DATETIME;
