-- =====================================================================================
-- PROYECTO: ZONE CONTROL — Laboratorio XYZ
-- MIGRACIÓN V6: Zonas operativas semilla (reales, no mocks)
-- DESCRIPCIÓN: Inserta las zonas que hasta ahora solo existían como datos
--              quemados en el frontend (AREA-A..D) más la zona de Calidad
--              (ZR-CAL01). A partir de esta migración son filas reales de
--              zone_control.areas_restringidas: visibles en kiosco, simulador
--              y catálogos, y utilizables en autorizaciones_zona (F-21).
-- IDEMPOTENCIA: ON CONFLICT (codigo) DO NOTHING — si la zona ya fue creada
--               por UI/Swagger con el mismo código, no se duplica.
-- NOTA: para retirar una zona a futuro usar baja lógica (activa = FALSE),
--       nunca DELETE (autorizaciones_zona tiene ON DELETE RESTRICT).
-- =====================================================================================

INSERT INTO zone_control.areas_restringidas (codigo, nombre, nivel_riesgo, descripcion, activa) VALUES
    ('AREA-A',  'Laboratorio de Síntesis Molecular (Área A)', 'ALTO',  'Zona crítica BSL-3.',             TRUE),
    ('AREA-B',  'Sala Limpia de Liofilización (Área B)',      'MEDIO', 'Zona estéril ISO 5.',             TRUE),
    ('AREA-C',  'Almacén Central (Área C)',                   'BAJO',  'Almacenamiento general.',         TRUE),
    ('AREA-D',  'Oficinas Administrativas (Área D)',          'BAJO',  'Zona de trabajo común.',          TRUE),
    ('ZR-CAL01','Laboratorio de Control de Calidad',          'MEDIO', 'Zona de análisis y control de calidad.', TRUE)
ON CONFLICT (codigo) DO NOTHING;
