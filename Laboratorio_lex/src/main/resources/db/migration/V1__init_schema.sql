-- =====================================================================================
-- PROYECTO: ZONE CONTROL — Laboratorio XYZ
-- SCRIPT: Data Definition Language (DDL) — PostgreSQL
-- DESCRIPCIÓN: Script de creación de esquema, restricciones, índices, triggers de
--              inmutabilidad y datos semilla, alineado a los requerimientos
--              funcionales (F-01 a F-35) y no funcionales (NF-01 a NF-12) del proyecto.
-- MOTOR: PostgreSQL 14+
-- ORDEN: Las tablas están creadas en orden de dependencia de Claves Foráneas (FK)
--        para permitir la ejecución secuencial del script sin errores.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 0. EXTENSIONES Y CONFIGURACIÓN INICIAL
-- -------------------------------------------------------------------------------------
-- pgcrypto habilita gen_random_uuid() para el uso de identificadores UUID en tablas
-- de alta sensibilidad/trazabilidad (historial_accesos, bitacora_auditoria).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Esquema dedicado del proyecto (buena práctica de aislamiento y mantenibilidad — NF-09).
CREATE SCHEMA IF NOT EXISTS zone_control;
SET search_path TO zone_control, public;

-- -------------------------------------------------------------------------------------
-- 1. TIPOS ENUMERADOS
-- -------------------------------------------------------------------------------------
-- El uso de ENUM en PostgreSQL garantiza integridad de dominio a nivel de motor,
-- evitando estados inválidos en columnas críticas (estado de usuarios, empleados, etc.).

CREATE TYPE estado_usuario AS ENUM ('ACTIVO', 'BLOQUEADO', 'INACTIVO');
CREATE TYPE estado_empleado AS ENUM ('ACTIVO', 'INACTIVO', 'BLOQUEADO');
CREATE TYPE resultado_acceso_enum AS ENUM ('AUTORIZADO', 'DENEGADO', 'NO_REGISTRADO');
CREATE TYPE tipo_operacion_enum AS ENUM ('CREACION', 'MODIFICACION', 'BLOQUEO', 'DESBLOQUEO', 'DESCARGA', 'ELIMINACION_LOGICA');
CREATE TYPE estado_sincronizacion_enum AS ENUM ('EXITOSO', 'EN_REINTENTO', 'FALLIDO');

-- -------------------------------------------------------------------------------------
-- 2. FUNCIÓN GENÉRICA: ACTUALIZACIÓN AUTOMÁTICA DE updated_at
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION zone_control.fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------------------------------
-- 3. FUNCIÓN GENÉRICA: BLOQUEO DE UPDATE/DELETE (INMUTABILIDAD — NF-11)
-- -------------------------------------------------------------------------------------
-- Se aplica sobre historial_accesos y bitacora_auditoria. Cualquier intento de
-- modificar o eliminar un registro desde la capa de aplicación (o manualmente)
-- lanzará una excepción, garantizando la trazabilidad exigida por auditoría.
CREATE OR REPLACE FUNCTION zone_control.fn_prevent_update_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operación no permitida: la tabla % es de solo lectura/inserción (inmutable) por regla NF-11.', TG_TABLE_NAME;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================================
-- 4. TABLA: roles  (F-06)
-- =====================================================================================
CREATE TABLE roles (
    id              SMALLSERIAL PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE
                        CHECK (nombre IN ('ADMINISTRADOR', 'GESTOR_PERSONAL', 'SUPERVISOR_ACCESOS')),
    descripcion     VARCHAR(150),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE roles IS 'Catálogo de roles internos del sistema (RBAC) — F-06.';

-- =====================================================================================
-- 5. TABLA: usuarios  (F-01, F-02, F-06, F-07, F-08, F-09, F-10, NF-04, NF-07)
-- =====================================================================================
CREATE TABLE usuarios (
    id                  BIGSERIAL PRIMARY KEY,
    documento           VARCHAR(20)  NOT NULL UNIQUE,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    correo              VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,              -- Hash BCrypt (NF-04). Nunca texto plano.
    estado              estado_usuario NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos   SMALLINT NOT NULL DEFAULT 0
                            CHECK (intentos_fallidos >= 0 AND intentos_fallidos <= 3),  -- Bloqueo tras 3 intentos (F-08)
    rol_id              SMALLINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- F-08: si el estado es BLOQUEADO, los intentos fallidos deben ser >= 3 (consistencia lógica)
    CONSTRAINT chk_usuarios_bloqueo CHECK (
        (estado = 'BLOQUEADO' AND intentos_fallidos >= 3) OR (estado <> 'BLOQUEADO')
    )
);

COMMENT ON TABLE usuarios IS 'Usuarios internos que acceden al software (Administrador, Gestor de Personal, Supervisor de Accesos).';
COMMENT ON COLUMN usuarios.password_hash IS 'Hash BCrypt de la contraseña. NUNCA almacenar ni loguear en texto plano (NF-04, NF-07).';

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_set_updated_at();

-- =====================================================================================
-- 6. TABLA: tokens_recuperacion  (F-05, NF-06)
-- =====================================================================================
CREATE TABLE tokens_recuperacion (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,     -- Se almacena el HASH del token, nunca el token en texto plano.
    expiracion      TIMESTAMPTZ NOT NULL,              -- Emitido con vigencia de 30 minutos (F-05).
    usado           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tokens_recuperacion IS 'Tokens temporales (30 min) para restablecimiento de contraseña — F-05.';

CREATE INDEX idx_tokens_recuperacion_usuario ON tokens_recuperacion(usuario_id);
-- Acelera la limpieza/consulta de tokens vigentes y evita escanear tokens ya usados/expirados.
CREATE INDEX idx_tokens_recuperacion_vigentes ON tokens_recuperacion(expiracion) WHERE usado = FALSE;

-- =====================================================================================
-- 7. TABLA: departamentos  (F-18)
-- =====================================================================================
CREATE TABLE departamentos (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     VARCHAR(255),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE departamentos IS 'Catálogo dinámico de departamentos de producción — F-18.';

CREATE TRIGGER trg_departamentos_updated_at
    BEFORE UPDATE ON departamentos
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_set_updated_at();

-- =====================================================================================
-- 8. TABLA: areas_restringidas  (F-18)
-- =====================================================================================
CREATE TABLE areas_restringidas (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    nivel_riesgo    VARCHAR(20)  NOT NULL DEFAULT 'MEDIO'
                        CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO', 'CRITICO')),
    descripcion     VARCHAR(255),
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE areas_restringidas IS 'Catálogo dinámico de zonas de bioseguridad/planta — F-18.';

CREATE TRIGGER trg_areas_restringidas_updated_at
    BEFORE UPDATE ON areas_restringidas
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_set_updated_at();

-- =====================================================================================
-- 9. TABLA: empleados  (F-11, F-12, F-16, F-17, F-34)
-- =====================================================================================
CREATE TABLE empleados (
    id                      BIGSERIAL PRIMARY KEY,
    tipo_documento          VARCHAR(10)  NOT NULL
                                CHECK (tipo_documento IN ('CC', 'CE', 'PASAPORTE', 'PPT', 'TI')),
    numero_documento        VARCHAR(20)  NOT NULL UNIQUE,   -- Único e inmutable por regla de negocio (F-12, F-16)
    nombres                 VARCHAR(100) NOT NULL,
    apellidos               VARCHAR(100) NOT NULL,
    correo                  VARCHAR(150),
    telefono                VARCHAR(20),
    departamento_id         INTEGER NOT NULL REFERENCES departamentos(id) ON DELETE RESTRICT,
    codigo_tarjeta_rfid     VARCHAR(50) UNIQUE,             -- Nullable: no todo empleado tiene tarjeta asignada aún (F-19)
    estado                  estado_empleado NOT NULL DEFAULT 'ACTIVO',
    motivo_cambio_estado    TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- F-17: el motivo de cambio de estado es obligatorio si el empleado no está ACTIVO.
    CONSTRAINT chk_empleados_motivo_estado CHECK (
        (estado <> 'ACTIVO' AND motivo_cambio_estado IS NOT NULL AND length(trim(motivo_cambio_estado)) > 0)
        OR (estado = 'ACTIVO')
    )
);

COMMENT ON TABLE empleados IS 'Personal operativo de planta sujeto a control de acceso — F-11, F-12, F-16, F-17.';
COMMENT ON COLUMN empleados.numero_documento IS 'Campo inmutable una vez creado el registro (regla de negocio F-16); protegido adicionalmente por trigger.';

CREATE TRIGGER trg_empleados_updated_at
    BEFORE UPDATE ON empleados
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_set_updated_at();

-- F-16: impedir la modificación del número de documento tras su creación.
CREATE OR REPLACE FUNCTION zone_control.fn_empleados_bloquear_doc_inmutable()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero_documento IS DISTINCT FROM OLD.numero_documento THEN
        RAISE EXCEPTION 'El número de documento del empleado es inmutable y no puede modificarse (F-16).';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_empleados_doc_inmutable
    BEFORE UPDATE ON empleados
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_empleados_bloquear_doc_inmutable();

-- Índices B-Tree de búsqueda frecuente (requisito explícito de rendimiento):
CREATE UNIQUE INDEX idx_empleados_numero_documento ON empleados(numero_documento);
CREATE UNIQUE INDEX idx_empleados_codigo_tarjeta_rfid ON empleados(codigo_tarjeta_rfid) WHERE codigo_tarjeta_rfid IS NOT NULL;
-- Soporta F-34: búsqueda por nombres/apellidos y filtro por departamento.
CREATE INDEX idx_empleados_nombres_apellidos ON empleados(apellidos, nombres);
CREATE INDEX idx_empleados_departamento ON empleados(departamento_id);

-- =====================================================================================
-- 10. TABLA: autorizaciones_zona  (F-21)
-- =====================================================================================
CREATE TABLE autorizaciones_zona (
    id                  BIGSERIAL PRIMARY KEY,
    empleado_id         BIGINT NOT NULL REFERENCES empleados(id) ON DELETE CASCADE,
    area_id             INTEGER NOT NULL REFERENCES areas_restringidas(id) ON DELETE RESTRICT,
    asignado_por        BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    fecha_asignacion    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_autorizaciones_empleado_area UNIQUE (empleado_id, area_id)
);

COMMENT ON TABLE autorizaciones_zona IS 'Matriz de permisos empleado-área restringida (autorización de ingreso) — F-21.';

CREATE INDEX idx_autorizaciones_empleado ON autorizaciones_zona(empleado_id);
CREATE INDEX idx_autorizaciones_area ON autorizaciones_zona(area_id);
-- Acelera la evaluación de permisos vigentes en la simulación de acceso (NF-01, < 1.5s).
CREATE INDEX idx_autorizaciones_activas ON autorizaciones_zona(empleado_id, area_id) WHERE activo = TRUE;

-- =====================================================================================
-- 11. TABLA: historial_accesos  (F-20, F-21, F-22, F-23, F-24, F-25, NF-01, NF-11)
-- =====================================================================================
CREATE TABLE historial_accesos (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_documento_ingresado  VARCHAR(20),
    codigo_tarjeta_ingresado    VARCHAR(50),
    empleado_id                 BIGINT REFERENCES empleados(id) ON DELETE SET NULL,   -- Nullable: personal no registrado
    area_id                     INTEGER REFERENCES areas_restringidas(id) ON DELETE SET NULL,
    resultado_acceso            resultado_acceso_enum NOT NULL,
    motivo_denegacion           VARCHAR(255),
    "timestamp"                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Debe existir al menos un identificador de entrada (documento o tarjeta) por cada intento.
    CONSTRAINT chk_historial_identificador CHECK (
        numero_documento_ingresado IS NOT NULL OR codigo_tarjeta_ingresado IS NOT NULL
    )
);

COMMENT ON TABLE historial_accesos IS 'Registro inmutable de cada intento de simulación de acceso — F-23, NF-11.';

-- Índices de rendimiento explícitos para consultas por fecha/resultado (F-24) y NF-01:
CREATE INDEX idx_historial_timestamp_resultado ON historial_accesos("timestamp", resultado_acceso);
CREATE INDEX idx_historial_empleado ON historial_accesos(empleado_id);
CREATE INDEX idx_historial_area ON historial_accesos(area_id);
CREATE INDEX idx_historial_documento_ingresado ON historial_accesos(numero_documento_ingresado);

-- NF-11: inmutabilidad — se impide UPDATE y DELETE a nivel de motor.
CREATE TRIGGER trg_historial_accesos_no_update
    BEFORE UPDATE ON historial_accesos
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_prevent_update_delete();

CREATE TRIGGER trg_historial_accesos_no_delete
    BEFORE DELETE ON historial_accesos
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_prevent_update_delete();

-- =====================================================================================
-- 12. TABLA: bitacora_auditoria  (F-31, F-32, F-33, NF-11)
-- =====================================================================================
CREATE TABLE bitacora_auditoria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "timestamp"         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id          BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,   -- Nullable: operaciones automáticas del sistema
    direccion_ip        INET,
    tipo_operacion      tipo_operacion_enum NOT NULL,
    modulo_tabla        VARCHAR(100) NOT NULL,
    valor_anterior      JSONB,
    valor_nuevo         JSONB
);

COMMENT ON TABLE bitacora_auditoria IS 'Bitácora inmutable de operaciones críticas del sistema — F-31, F-32, NF-11.';

-- Soporta F-33: consulta y filtrado de eventos de auditoría.
CREATE INDEX idx_bitacora_timestamp ON bitacora_auditoria("timestamp");
CREATE INDEX idx_bitacora_usuario ON bitacora_auditoria(usuario_id);
CREATE INDEX idx_bitacora_tipo_operacion ON bitacora_auditoria(tipo_operacion);
CREATE INDEX idx_bitacora_modulo_tabla ON bitacora_auditoria(modulo_tabla);
-- Índices GIN para búsquedas eficientes dentro de los payloads JSONB.
CREATE INDEX idx_bitacora_valor_anterior_gin ON bitacora_auditoria USING GIN (valor_anterior);
CREATE INDEX idx_bitacora_valor_nuevo_gin ON bitacora_auditoria USING GIN (valor_nuevo);

-- NF-11: inmutabilidad — se impide UPDATE y DELETE a nivel de motor.
CREATE TRIGGER trg_bitacora_auditoria_no_update
    BEFORE UPDATE ON bitacora_auditoria
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_prevent_update_delete();

CREATE TRIGGER trg_bitacora_auditoria_no_delete
    BEFORE DELETE ON bitacora_auditoria
    FOR EACH ROW EXECUTE FUNCTION zone_control.fn_prevent_update_delete();

-- =====================================================================================
-- 13. TABLA: registro_sincronizacion_socio  (F-26, F-27, F-28, F-29, F-30)
-- =====================================================================================
CREATE TABLE registro_sincronizacion_socio (
    id                          BIGSERIAL PRIMARY KEY,
    periodo_inicio              TIMESTAMPTZ NOT NULL,
    periodo_fin                 TIMESTAMPTZ NOT NULL,
    departamento_id             INTEGER REFERENCES departamentos(id) ON DELETE SET NULL,  -- Nullable: consolidado global
    payload_json                JSONB NOT NULL,
    estado                      estado_sincronizacion_enum NOT NULL DEFAULT 'EN_REINTENTO',
    intentos_realizados         SMALLINT NOT NULL DEFAULT 0
                                    CHECK (intentos_realizados >= 0 AND intentos_realizados <= 3),  -- Política de 3 reintentos (F-28)
    codigo_respuesta_http       SMALLINT,
    fecha_envio                 TIMESTAMPTZ,
    fecha_proximo_reintento     TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_sincronizacion_periodo CHECK (periodo_fin >= periodo_inicio)
);

COMMENT ON TABLE registro_sincronizacion_socio IS 'Trazabilidad de envíos JSON al socio internacional (webhook/API) — F-26 a F-30.';

CREATE INDEX idx_sincronizacion_estado ON registro_sincronizacion_socio(estado);
CREATE INDEX idx_sincronizacion_departamento ON registro_sincronizacion_socio(departamento_id);
CREATE INDEX idx_sincronizacion_periodo ON registro_sincronizacion_socio(periodo_inicio, periodo_fin);
-- Acelera el job de reintentos automáticos (F-28) al buscar sincronizaciones pendientes.
CREATE INDEX idx_sincronizacion_pendientes ON registro_sincronizacion_socio(fecha_proximo_reintento)
    WHERE estado = 'EN_REINTENTO';
CREATE INDEX idx_sincronizacion_payload_gin ON registro_sincronizacion_socio USING GIN (payload_json);

-- =====================================================================================
-- 14. DATOS SEMILLA (SEED DATA)
-- =====================================================================================

-- 14.1 Roles por defecto (F-06)
INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR',      'Administra usuarios internos, roles y configuración general del sistema.'),
    ('GESTOR_PERSONAL',    'Gestiona el personal operativo, sus datos y autorizaciones de ingreso a zonas.'),
    ('SUPERVISOR_ACCESOS', 'Supervisa la actividad de acceso, audita eventos y genera reportes.');

-- 14.2 Departamentos de prueba (F-18)
INSERT INTO departamentos (codigo, nombre, descripcion, activo) VALUES
    ('DEP-PROD', 'Producción',        'Departamento de producción de medicamentos de alto costo.', TRUE),
    ('DEP-CAL',  'Control de Calidad','Departamento encargado del aseguramiento y control de calidad.', TRUE);

-- 14.3 Áreas restringidas de prueba (F-18)
INSERT INTO areas_restringidas (codigo, nombre, nivel_riesgo, descripcion, activa) VALUES
    ('ZR-LAB01', 'Laboratorio de Bioseguridad 1', 'ALTO',  'Zona de manipulación de principios activos de alto riesgo.', TRUE),
    ('ZR-EMP01', 'Zona de Empaque 1',             'MEDIO', 'Área de empaque y etiquetado de producto terminado.', TRUE);

-- 14.4 Usuario administrador inicial
-- NOTA: el valor de password_hash es SIMBÓLICO/PLACEHOLDER únicamente para fines de
-- inicialización del entorno. Debe ser reemplazado por un hash BCrypt real
-- generado por la aplicación (nunca insertar contraseñas en texto plano — NF-04, NF-07).
INSERT INTO usuarios (documento, nombres, apellidos, correo, password_hash, estado, intentos_fallidos, rol_id) VALUES
    (
        '0000000001',
        'Admin',
        'Principal',
        'admin@laboratorioxyz.com',
        '$2a$12$PLACEHOLDER.BCRYPT.HASH.GENERADO.POR.LA.APLICACION........',
        'ACTIVO',
        0,
        (SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR')
    );

-- =====================================================================================
-- FIN DEL SCRIPT
-- =====================================================================================