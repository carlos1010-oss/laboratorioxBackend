SET search_path TO zone_control;
-- 14. INSERCIÓN DE DATOS INICIALES

-- =====================================================================================
-- 14. DATOS SEMILLA (SEED DATA)
-- =====================================================================================

-- 14.1 Roles por defecto (F-06)
INSERT INTO roles (nombre, descripcion, created_at) VALUES
    ('ADMINISTRADOR',      'Administra usuarios internos, roles y configuración general del sistema.', CURRENT_TIMESTAMP),
    ('GESTOR_PERSONAL',    'Gestiona el personal operativo, sus datos y autorizaciones de ingreso a zonas.', CURRENT_TIMESTAMP),
    ('SUPERVISOR_ACCESOS', 'Supervisa la actividad de acceso, audita eventos y genera reportes.', CURRENT_TIMESTAMP);

-- 14.2 Departamentos de prueba (F-18)
INSERT INTO departamentos (codigo, nombre, descripcion, activo, created_at, updated_at) VALUES
    ('DEP-PROD', 'Producción',        'Departamento de producción de medicamentos de alto costo.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEP-CAL',  'Control de Calidad','Departamento encargado del aseguramiento y control de calidad.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 14.3 Áreas restringidas de prueba (F-18)
INSERT INTO areas_restringidas (codigo, nombre, nivel_riesgo, descripcion, activa, created_at, updated_at) VALUES
    ('ZR-LAB01', 'Laboratorio de Bioseguridad 1', 'ALTO',  'Zona de manipulación de principios activos de alto riesgo.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ZR-EMP01', 'Zona de Empaque 1',             'MEDIO', 'Área de empaque y etiquetado de producto terminado.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 14.4 Usuario administrador inicial
INSERT INTO usuarios (documento, nombres, apellidos, correo, password_hash, estado, intentos_fallidos, rol_id, token_version, created_at, updated_at) VALUES
    (
        '0000000001',
        'Admin',
        'Principal',
        'admin@laboratorioxyz.com',
        '$2a$10$KobyJRkAYA8qA1EQekhe6ued8fWd4W42JvuDKHQcbjPlRdbwklKx6',
        'ACTIVO',
        0,
        (SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR'),
        0,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

-- =====================================================================================
-- FIN DEL SCRIPT
-- =====================================================================================