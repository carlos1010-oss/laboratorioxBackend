-- ============================================================================
-- MIGRACIÓN V5: Índice para bitacora_auditoria.direccion_ip (F-32)
-- Permite búsquedas eficientes por IP de origen en auditoría.
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_bitacora_direccion_ip 
ON zone_control.bitacora_auditoria (direccion_ip);

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================