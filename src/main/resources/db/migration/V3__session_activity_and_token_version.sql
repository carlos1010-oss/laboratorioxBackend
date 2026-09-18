-- =====================================================================================
-- PROYECTO: ZONE CONTROL — Laboratorio XYZ
-- MIGRACIÓN: V3
-- DESCRIPCIÓN: Soporta el control de sesión exigido por F-03 (expiración por inactividad
--              de 5 minutos) y F-04 (invalidación del token al cerrar sesión), añadiendo
--              dos columnas de control a la tabla de usuarios.
-- MOTOR: PostgreSQL
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. CONTROL DE SESIÓN EN USUARIOS (F-03, F-04)
-- -------------------------------------------------------------------------------------
ALTER TABLE usuarios
    ADD COLUMN ultima_actividad TIMESTAMPTZ,
    ADD COLUMN token_version    INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN usuarios.ultima_actividad IS
    'Marca de la última petición autenticada; si supera los minutos de inactividad la sesión expira (F-03).';
COMMENT ON COLUMN usuarios.token_version IS
    'Versión del token vigente; se incrementa al cerrar sesión para invalidar los tokens ya emitidos (F-04).';

-- =====================================================================================
-- FIN DE LA MIGRACIÓN
-- =====================================================================================
