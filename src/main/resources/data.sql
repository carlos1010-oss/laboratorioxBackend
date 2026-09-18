-- Insertar rol inicial (solo requiere created_at)
INSERT INTO zone_control.roles (id, nombre, descripcion, created_at) 
VALUES (1, 'ADMINISTRADOR', 'Acceso total al sistema', CURRENT_TIMESTAMP);

-- Insertar usuario inicial (requiere created_at y updated_at)
INSERT INTO zone_control.usuarios (
    documento, nombres, apellidos, correo, password_hash, estado, rol_id, intentos_fallidos, created_at, updated_at
) VALUES (
    '10101010', 
    'Carlos David', 
    'Camacho', 
    'admin@laboratorioxyz.com', 
    '$2a$10$E27M24oT/6mD.4E9/yA2/.8E9b/X1m6X.Q5/Q2d5c3d4e5f6g7h8i', 
    'ACTIVO', 
    1, 
    0, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);