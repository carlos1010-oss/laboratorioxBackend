-- ============================================================================
-- MIGRACIÓN V4: Alineación del esquema — bitacora_auditoria.direccion_ip
-- Cambia el tipo de la columna de INET a VARCHAR(45).
-- Motivo: la aplicación registra la IP real del cliente (F-31/F-32), incluida
-- la primera dirección de cadenas X-Forwarded-For cuando hay proxies, lo que no
-- siempre es un INET válido. Este cambio existía únicamente como ALTER manual en
-- la BD local; esta migración lo versiona para que cualquier entorno
-- (socio/frontend) quede idéntico.
-- La operación es idempotente: si la columna ya es VARCHAR(45) no modifica nada.
-- ============================================================================

ALTER TABLE zone_control.bitacora_auditoria
    ALTER COLUMN direccion_ip TYPE VARCHAR(45)
    USING direccion_ip::text;