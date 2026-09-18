-- =====================================================================================
-- PROYECTO: ZONE CONTROL — Laboratorio XYZ
-- MIGRACIÓN: V2
-- DESCRIPCIÓN: Incorpora los campos exigidos por las historias de usuario HU-005 y HU-013
--              (IP de origen y navegador en cada intento de acceso) y crea la tabla de
--              configuración de la exportación periódica al socio internacional (HU-015).
-- MOTOR: PostgreSQL
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. HISTORIAL DE ACCESOS: IP de origen y User-Agent (HU-005, HU-013)
-- -------------------------------------------------------------------------------------
ALTER TABLE historial_accesos
    ADD COLUMN ip_origen  INET,
    ADD COLUMN user_agent VARCHAR(255);

COMMENT ON COLUMN historial_accesos.ip_origen  IS 'Dirección IP desde la que se originó el intento de acceso (HU-013).';
COMMENT ON COLUMN historial_accesos.user_agent IS 'Navegador/cliente que originó el intento de acceso (HU-013).';

-- -------------------------------------------------------------------------------------
-- 2. CONFIGURACIÓN DE EXPORTACIÓN AL SOCIO INTERNACIONAL (HU-015)
-- -------------------------------------------------------------------------------------
CREATE TABLE configuracion_exportacion (
    id                          SMALLSERIAL PRIMARY KEY,
    frecuencia                  VARCHAR(20) NOT NULL DEFAULT 'SEMANAL'
                                    CHECK (frecuencia IN ('DIARIA', 'SEMANAL', 'MENSUAL')),
    dia_semana                  SMALLINT CHECK (dia_semana BETWEEN 1 AND 7),   -- 1=Lunes ... 7=Domingo
    hora                        TIME NOT NULL DEFAULT '23:59',
    formato                     VARCHAR(10) NOT NULL DEFAULT 'JSON'
                                    CHECK (formato IN ('JSON')),
    url_destino                 VARCHAR(500),
    correo_alerta               VARCHAR(150),
    activo                      BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_proxima_ejecucion     TIMESTAMPTZ,
    ultima_ejecucion            TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE configuracion_exportacion IS 'Parámetros de la exportación periódica al socio internacional — HU-015, F-26 a F-30.';

CREATE TRIGGER trg_configuracion_exportacion_updated_at
    BEFORE UPDATE ON configuracion_exportacion
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_set_updated_at();

-- Registro único de configuración (desactivada por defecto hasta que el Supervisor la configure)
INSERT INTO configuracion_exportacion (frecuencia, dia_semana, hora, formato, activo)
VALUES ('SEMANAL', 7, '23:59', 'JSON', FALSE);

-- -------------------------------------------------------------------------------------
-- 3. CONTRASEÑA INICIAL DEL ADMINISTRADOR (F-01, HU-001)
-- -------------------------------------------------------------------------------------
-- El script V1 insertó un hash PLACEHOLDER no funcional. Se reemplaza por un hash BCrypt
-- real de la contraseña temporal 'Admin123' (cumple: 8+ caracteres, mayúscula, minúscula
-- y alfanuméricos). Debe cambiarse en el primer inicio de sesión.
UPDATE usuarios
SET password_hash = '$2a$10$WdidcbAyRzDXuDpaHKmL2OZRdlsX/eaevwjxJFtNwcCq4YD0NhND2'
WHERE password_hash LIKE '$2a$12$PLACEHOLDER%';

-- =====================================================================================
-- FIN DE LA MIGRACIÓN
-- =====================================================================================
