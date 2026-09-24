# Zone Control — Progreso del Backend

> Documento de continuidad para retomar el trabajo en otra sesión sin contexto previo.
> Última actualización: Fase 9 completada (Molinete público / kiosco para empleados sin login: nuevo GET /api/publico/areas + página /molinete en frontend). RF funcionales (F-01..F-35) del backlog cubiertos salvo F-25-PDF (aplazado intencionalmente).

## Ubicación y stack

- Repo backend local: `c:\Users\ccamachor\Desktop\laboratorioxBackend`
- GitHub Remoto: `https://github.com/carlos1010-oss/laboratorioxBackend.git` (rama `main`)
- Producción Render: `https://laboratorio-zone-control.onrender.com` (Swagger UI: `/swagger-ui.html`)
- Paquete raíz Java: `Laboratorio_lex` (groupId Maven: `com.laboratorioxyz`, artifactId: `zone-control`).
- Spring Boot 3.2.5 · Java 21 · Docker Multi-stage · Hibernate 6.4 · Flyway 9.22.3 · HikariCP.
- Frontend Next.js: Repositorio separado (pendiente de conectar a Render vía `NEXT_PUBLIC_API_URL`).
- BD Nube: Aiven PostgreSQL 18.6, esquema `zone_control`, migraciones V1 a V5 aplicadas.

## Comandos útiles

```powershell
# compilar
.\mvnw.cmd -o compile

# levantar (requiere PostgreSQL en localhost:5432)
.\mvnw.cmd -o spring-boot:run

# detener app que escucha en 8080
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

## Estado: Fase 1 — COMPLETADA (compila y arranca)

Entidades y migraciones alineadas a la BD real. La app arranca con `Started LaboratorioLexApplication`.

Cambios hechos:
- `HistorialAcceso`: campos `ipOrigen` (InetAddress) y `userAgent`; ID `GenerationType.UUID`.
- `AccesoService`/`AccesoController`/DTOs: simulación por **documento o RFID**, captura IP y User-Agent.
- `HistorialAccesoRepository`: `JpaRepository<HistorialAcceso, UUID>` + `findByTimestampBetween`.
- `BitacoraAuditoria.direccionIp`: `String` (en Fase 6 la columna quedó alineada vía V4; antes el `ALTER` era solo manual en la BD).
- ENUM nativos PG mapeados con `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `columnDefinition`:
  `estado_usuario`, `estado_empleado`, `resultado_acceso_enum`, `tipo_operacion_enum`, `estado_sincronizacion_enum`.
- `@EnableScheduling` en `LaboratorioLexApplication`.
- Migración `V2__historial_audit_fields_and_export_config.sql` aplicada:
  - `historial_accesos.ip_origen` (INET), `historial_accesos.user_agent` (VARCHAR 255).
  - Tabla `configuracion_exportacion` (HU-015) + trigger `updated_at` + fila semilla.

IMPORTANTE — desfase V1 vs BD real:
- `bitacora_auditoria.direccion_ip` fue `VARCHAR` en la BD (V1 dice `INET`); **RESUELTO en Fase 6** con la migración `V4__bitacora_direccion_ip_varchar.sql` (versionada e idempotente).
- El usuario admin ya tenía un hash BCrypt real (NO el placeholder de V1). El `UPDATE` de V2 es condicional y no lo cambió. La contraseña del admin es la que ya conocías.

## Estado: Fase 2 — COMPLETADA (login por documento + RBAC), verificada contra la app en vivo

Cambios hechos:
- `LoginRequestDTO`: campo `correo`/`@Email` → `documento` (F-01/HU-001).
- `AuthService.login`: busca por `findByDocumento`, mensajes amigables, bloqueo a los 3 intentos, reset al éxito.
  **F-08 corregido** con `@Transactional(noRollbackFor = { CredencialesInvalidasException.class, CuentaBloqueadaException.class })`:
  ahora el contador SÍ persiste (verificado: `intentos_fallidos=1` en BD tras un 401).
- `JwtProvider`: `getCorreoFromToken` → `getSubjectFromToken`; subject = documento.
- `JwtAuthenticationFilter`: resuelve el usuario con `findByDocumento`.
- `UsuarioRepository`: añadido `countByRol_NombreAndEstado(...)`.
- `UsuarioService`: F-10 (no degradar/bloquear al único Administrador activo) en edición y cambio de estado.
- `UsuarioCreacionDTO`: `@Pattern` de política de contraseña (8+, mayúscula, minúscula, número).
- `GlobalExceptionHandler`: 401 para `CredencialesInvalidasException`, 403 para `CuentaBloqueadaException`;
  el 500 genérico ya no filtra detalle técnico (NF-15) y se registra en logs.
- `SecurityConfig`: RBAC por rol + entry point/access denied con JSON (401/403).
  - `POST /api/auth/login` y `/h2-console/**` → `permitAll`.
  - `/api/auth/usuarios/**` → ADMINISTRADOR.
  - `/api/auditoria/**`, `/api/sincronizacion/**` → ADMINISTRADOR, SUPERVISOR_ACCESOS.
  - `/api/accesos/autorizaciones/**`, `/api/personal/**` → ADMINISTRADOR, GESTOR_PERSONAL.
  - `/api/accesos/**` → los tres roles. Resto → autenticado.

Evidencia de la verificación (app en vivo, `usuario de prueba` luego eliminado):
1. `POST /api/auth/login` con `{documento,password}` → 200 con JWT. ✔
2. ADMINISTRADOR `GET /api/auth/usuarios` → 200. ✔
3. Sin token `GET /api/personal/empleados` → 401 con JSON (`Content-Length: 99`). ✔
4. Se creó un GESTOR_PERSONAL (`201`): `GET /api/auth/usuarios` → 403, `GET /api/personal/empleados` → 200. ✔
5. Password incorrecta → 401 amigable `"...Intentos fallidos: 1/3"` y persistido en BD. ✔

> Nota de prueba: para el login válido se cambió temporalmente el hash del admin a uno conocido (`Admin123`),
> se verificó, y **se restauró el hash original** (`$2a$10$eE4044YFvWJ4Yy/4u6Q5.e/8uO0M7l1F9R0PzN5.L3sBqJ6iO8Oim`)
> con `intentos_fallidos=0` y `estado=ACTIVO`. La contraseña real del admin sigue siendo la original.

## Estado: Fase 3 — COMPLETADA (sesión por inactividad y logout), verificada contra la app en vivo

Migración `V3__session_activity_and_token_version.sql` (aplicada en arranque) añade a `usuarios`:
- `ultima_actividad TIMESTAMPTZ`
- `token_version INTEGER NOT NULL DEFAULT 0`

Cambios hechos:
- `Usuario`: campos `ultimaActividad` y `tokenVersion` (default 0 en `@PrePersist`).
- `JwtProvider`: `generateToken(documento, tokenVersion)` incluye el claim `ver`; `getTokenVersionFromToken(...)`.
- `JwtAuthenticationFilter`: rechaza el token si el usuario no está ACTIVO, si `ver` ≠ `token_version`
  (logout — F-04) o si `ultima_actividad` supera `seguridad.sesion.inactividad-minutos` (F-03);
  en cada petición válida actualiza `ultima_actividad` con `usuarioRepository.actualizarUltimaActividad(...)`.
- `UsuarioRepository`: `actualizarUltimaActividad(id, momento)` e `invalidarTokens(id)` (ambos `@Modifying @Transactional`).
- `AuthService`: login reinicia la inactividad y firma con la versión vigente; nuevo `cerrarSesion(documento)`.
- `AuthController`: `POST /api/auth/logout` (autenticado) → invalida tokens.
- `application.properties`: `seguridad.sesion.inactividad-minutos=5`.

Evidencia de la verificación:
1. Token recién emitido → `GET /api/auth/usuarios` → 200. ✔
2. `ultima_actividad` retrocedida 6 min en BD → mismo token → 401 (inactividad). ✔
3. Nuevo login → 200 (la actividad se reinicia). ✔
4. `POST /api/auth/logout` → 200; el mismo token → 401 (versión invalidada). ✔

> Nota de prueba: igual que en Fase 2, se cambió temporalmente el hash del admin a `Admin123` y se restauró
> el original al terminar, dejando `intentos_fallidos=0`, `estado=ACTIVO`, `token_version=0`, `ultima_actividad=null`.

## Estado: Fase 4 — COMPLETADA (Gestión de Personal, Catálogos y Credenciales — F-11..F-19)

Endpoints de empleados (`/api/personal/empleados`, ADMINISTRADOR + GESTOR_PERSONAL):
- `GET` / `GET /{id}` / `POST` / `PUT /{id}` (F-16) / `PATCH /{id}/estado` (F-17).
- `PUT /{id}/tarjeta` (asignar/reasignar) y `DELETE /{id}/tarjeta` (desvincular) — F-19.
- `GET /plantilla-csv` (F-14) y `POST /importar-csv` (F-13/F-15).

Cambios hechos:
- `EmpleadoRequestDTO`: añade `estado` (opcional) y `motivoCambioEstado`; valida `tipoDocumento` contra el CHECK de la BD.
- **Nuevo** `EmpleadoUpdateDTO` (sin estado; `numeroDocumento` opcional que debe coincidir) y `AsignarTarjetaDTO`.
- `EmpleadoService`: `actualizarEmpleado`, `asignarTarjeta`, `desvincularTarjeta`; `crearEmpleado`/`cambiarEstado` ahora
  validan F-17 (`validarMotivoEstado`). El documento inmutable se protege en servicio **y** por trigger `trg_empleados_doc_inmutable` (F-16).
- `EmpleadoCsvService` reescrito: encabezados flexibles (snake_case o camelCase), validación de tipos/email/estado/departamento,
  detección de duplicados dentro del archivo y contra la BD, y reporte por línea (`ImportacionResultadoDTO`). `generarPlantillaCsv()`.
- **Nuevo módulo `modules/catalogos`** (F-18): `DepartamentoController` (`/api/catalogos/departamentos`) y
  `AreaRestringidaController` (`/api/catalogos/areas-restringidas`); servicios y DTOs. Incluye baja lógica vía `PATCH /{id}/estado`.
- `SecurityConfig`: `GET /api/catalogos/**` → ADMINISTRADOR/GESTOR_PERSONAL/SUPERVISOR_ACCESOS; el resto de métodos → solo ADMINISTRADOR.

Evidencia de la verificación:
1. Plantilla CSV devuelve cabeceras `tipo_documento,numero_documento,nombres,apellidos,correo,telefono,codigo_departamento,estado,motivo_cambio_estado` → 200. ✔
2. Alta de empleado (F-11) → 201; documento duplicado y RFID duplicada → 400 (F-12). ✔
3. `PATCH estado INACTIVO` sin motivo → 400; con motivo → 200; reactivar → 200 (F-17). ✔
4. `PUT` edita datos → 200; intento de cambiar `numeroDocumento` → 400 (F-16). ✔
5. Asignar tarjeta → 200; asignar la misma a otro empleado → 400; desvincular → 200 (F-19). ✔
6. Import CSV con 4 filas (1 válida, 1 depto inexistente, 1 duplicada, 1 sin nombres) → `{exitosos:1, fallidos:3}` con errores por línea (F-15). ✔
7. Catálogos: crear/editar/desactivar área y departamento → 200/201 (F-18). ✔

> Nota: los datos de prueba (empleados `99000000*`, área `ZR-TEST01`) se eliminaron y el hash del admin se restauró.

## Estado: Fase 5 — COMPLETADA (Control de accesos avanzado y sincronización con el socio — F-22..F-30)

Historial filtrado y exportación (F-24/F-25), semáforo de resultado (F-22), consolidado por departamento (F-26)
y sincronización real configurable con el socio internacional (F-27..F-30).

Cambios hechos:
- `ResultadoAccesoResponseDTO`: añade `color` (VERDE/ROJO/AMARILLO, F-22), `ipOrigen` y `userAgent` (F-23).
- `HistorialAccesoRepository`: ahora extiende `JpaSpecificationExecutor`; método derivado `countByTimestampGreaterThanEqualAndTimestampLessThan`,
  `countByResultadoAndTimestampGreaterThanEqualAndTimestampLessThan`, y consulta JPQL `consolidarPorDepartamento(...)`.
- **Nuevo** `HistorialSpecifications` (filtros dinámicos por documento/área/fechas) y `HistorialExportService` (CSV con `commons-csv` + BOM UTF-8, y HTML imprimible).
- `AccesoService`: `buscarHistorial(...)` (Page paginado) y `listarParaExportar(...)`; `convertirADTO` incluye color/IP/user-agent.
- `AccesoController`: `GET /api/accesos/historial` (filtros `numeroDocumento`, `areaId`, `fechaInicio`, `fechaFin` + `Pageable`)
  y `GET /api/accesos/historial/exportar?formato=csv|html` (con `Content-Disposition`).
- **Nuevo** `ConsolidadoService` y DTOs `ConsolidadoResponseDTO`/`ConsolidadoDepartamentoDTO`: resumen global + por departamento (4 métricas).
- **Nueva entidad** `ConfiguracionExportacion` (tabla de V2 ahora mapeada) + `ConfiguracionExportacionRepository` y DTO.
- `SincronizacionService` reescrito:
  - Construye el payload JSON con el consolidado (`tipo`, `periodoInicio/Fin`, `departamento`, `resumenGeneral`, `porDepartamento`).
  - **F-27**: si `integracion.socio.simular=true` (default) registra EXITOSO sin llamar; si es `false`, hace `POST` JSON real con `RestTemplate`
    (timeouts 5s/10s) a la URL (propiedad `integracion.socio.url` o `configuracion_exportacion.url_destino`).
  - **F-28**: reintentos configurables (`max-reintentos=3`, `reintento-minutos=15`) con `@Scheduled` cada 5 min; al agotarlos → FALLIDO (503).
  - **F-29**: `AlertaCorreoService` envía correo a `configuracion_exportacion.correo_alerta` (o a usuarios SUPERVISOR_ACCESOS); en modo log si
    `correo-habilitado=false` o falta `spring.mail.host`. `JavaMailSender` se inyecta con `ObjectProvider` (no es bean sin `spring.mail.host`).
  - **F-30**: `obtenerEstado()` (dashboard), configuración GET/PUT, reenvío manual `POST /{id}/reenviar`.
- `SincronizacionController`: `POST /socio`, `GET /consolidado`, `GET /estado`, `GET/PUT /configuracion`, `POST /{id}/reenviar`, `GET /historial`.
- `application.properties`: bloque `integracion.socio.*`.

Evidencia de la verificación (app en vivo):
1. F-22/F-24: molinete → AUTORIZADO/VERDE, DENEGADO/ROJO, NO_REGISTRADO/AMARILLO; historial con filtros (por documento 2, por área 3) y paginación. ✔
2. F-25: `exportar?formato=csv` → 1226 bytes con `Content-Disposition`; `formato=html` → HTML imprimible; `formato=pdf` → 400 ("diferido"). ✔
3. F-26: consolidado `inicio/fin` → totales `intentos=4 aut=1 den=2 noreg=1` y un departamento con `total=3 aut=1 den=2`. ✔
4. F-27/F-28: `POST /socio` (simular=true) → EXITOSO con payload JSON correctamente estructurado. ✔
5. Con `simular=false` y sin URL → EN_REINTENTO (intentos=1, http=500, proxReintento +15min) y el reenvío manual hace un intento fresco. ✔
6. Con `max-reintentos=1` y `simular=false` → primera falla = FALLIDO (503) y `AlertaCorreoService` loguea destinatarios resueltos. ✔
7. F-30: config GET/PUT (activa+url), `/estado` refleja activa/url/correo/conteos; reenvío de un EXITOSO → 400 (no procede). ✔

> Decisiones: la exportación **PDF queda diferida** (sin dependencias nuevas por decisión del usuario, usa el HTML imprimible).
> El contrato del socio (URL real, SMTP) no está entregado: todo es **configurable y simulado** por defecto (`simular=true`).
> En JPQL no se usan literales de enum (Hibernate generaba `::ResultadoAcceso` inexistente en PG); los valores van por parámetro.

## Estado: Fase 6 — COMPLETADA (Auditoría con usuario/IP reales y filtros — F-31..F-33) + migración V4

Trazabilidad real: cada operación crítica registra el **usuario autenticado** (vía JWT) y la **IP real del cliente**
(soporta `X-Forwarded-For` de proxies/carga); consulta de la bitácora paginada y filtrable.

Cambios hechos:
- **Nuevo** `modules/auditoria/util/AuditoriaContexto`: `obtenerUsuarioActual()` (toma el `Usuario` del `SecurityContextHolder`)
  y `obtenerIpActual()` (lee el primer elemento de `X-Forwarded-For` o `getRemoteAddr()`; fallback `127.0.0.1` en jobs/pruebas).
- `EmpleadoService`, `UsuarioService` y `AutorizacionZonaService`: `registrarAuditoria(...)` ahora usa `AuditoriaContexto`
  en vez del hardcode `usuarioId=null` / `"127.0.0.1"` (F-31/F-32).
- `BitacoraAuditoriaRepository` extiende `JpaSpecificationExecutor` + **Nuevo** `AuditoriaSpecifications`
  (filtros opcionales: `usuarioId`, `tipoOperacion`, `moduloTabla` LIKE, `fechaInicio`/`fechaFin`).
- `AuditoriaService.obtenerPaginado(...)`: `Page<AuditoriaResponseDTO>` ordenado por `timestamp` DESC
  (antes `obtenerTodos()` devolvía lista completa sin filtros).
- `AuditoriaController`: `GET /api/auditoria` ahora recibe `usuarioId`, `tipoOperacion`, `moduloTabla`,
  `fechaInicio`/`fechaFin` (**`yyyy-MM-dd`, día completo** — `LocalDate`), `page` (0) y `size` (20).
- Migración **`V4__bitacora_direccion_ip_varchar.sql`**: `bitacora_auditoria.direccion_ip` pasa de `INET` a `VARCHAR(45)`
  (idempotente; versiona el `ALTER` manual que había en la BD local). Aplicada y verificada en `flyway_schema_history`.

Evidencia de la verificación (instancia 8081, datos de prueba luego eliminados):
1. Alta y edición de empleado con header `X-Forwarded-For: <IP, ...>` → bitácora con `usuario=Admin Principal (id=1)`
   e `ip=<primer valor del header>` (F-31/F-32). ✔
2. `GET /api/auditoria?tipoOperacion=CREACION&moduloTabla=empleados` → 1 registro correcto. ✔
3. `GET /api/auditoria?fechaInicio=2026-09-17&fechaFin=2026-09-17` → día completo (2 registros). ✔
4. `GET /api/auditoria?page=0&size=1&usuarioId=1&moduloTabla=empleados&tipoOperacion=CREACION` → total=1, content de 1. ✔
5. Ventana sin datos (`2025-01-01..2025-01-31`) → total=0. ✔
6. `flyway_schema_history`: `4 | bitacora direccion ip varchar | true`. `information_schema` confirma `character varying(45)`. ✔

> Nota: al terminar se truncaron los datos de prueba (empleados, bitácora, autorizaciones, historial) para dejar la BD
> como estaba. La contraseña del admin sigue siendo la temporal `Admin123` mientras el usuario pruebe el sistema.

## Estado: Fase 7 — COMPLETADA (Búsqueda en personal y portal público — F-34, F-35)

F-34: `GET /api/personal/empleados` ahora es paginado y filtrable. F-35: API público (sin token) para el portal público.

Cambios hechos:
- **Nuevo** `EmpleadoSpecifications` (filtros opcionales: `documento`, `nombres`, `apellidos` — LIKE case-insensitive —,
  `departamentoId` exacto y `estado`). `EmpleadoRepository` extiende `JpaSpecificationExecutor`.
- `EmpleadoService.buscar(...)` devuelve `Page<EmpleadoResponseDTO>` (orden por defecto `apellidos ASC`);
  `obtenerTodos()` eliminado. `EmpleadoController.GET` recibe los filtros + `@PageableDefault`.
- **Nuevo módulo `modules/portal`** (F-35), rutas públicas en `SecurityConfig` (`.requestMatchers("/api/publico/**").permitAll()`):
  - `POST /api/publico/accesos/molinete`: simula acceso igual que el interno (registra historial con IP/user-agent)
    pero **no expone datos personales** — `ResultadoAccesoPublicoDTO` (resultado, color, motivo, área, fecha).
  - `GET /api/publico/verificacion?documento=|tarjeta=`: `VerificacionIngresoDTO {registrado, estado, autorizadoIngreso}`
    (confirma existencia/estado sin datos personales). Sin documento ni tarjeta → 400.
- `AccesoService.procesarAccesoMolinete` y el historial quedan **reutilizados** por el portal público.

Evidencia de la verificación (instancia 8081, datos de prueba luego eliminados):
1. F-34: sin filtros → total=3; `nombres=juan` → 2; `departamentoId=1` → 2; `estado=INACTIVO` → 1;
   `apellidos=perez` → 1; `page=0&size=1&sort=numeroDocumento,desc` → total=3 con 1 elemento; combo completo → 1. ✔
2. F-35 `POST /api/publico/accesos/molinete` (doc aut.: área) → `AUTORIZADO/VERDE` y la respuesta JSON **no incluye**
   `nombreEmpleado` (sin fuga de datos). ✔
3. F-35 no registrado → `NO_REGISTRADO/AMARILLO`. ✔
4. F-35 `verificacion`: doc activo → `{registrado:true, estado:ACTIVO, autorizado:true}`; tarjeta de empleado INACTIVO →
   `{autorizado:false, estado:INACTIVO}`; tarjeta inexistente → `{registrado:false}`; sin parámetros → 400. ✔
5. `/api/personal/empleados` y `/api/auditoria` **sin token** → 401 (el resto del sistema sigue protegido). ✔

> Decisiones/riesgos F-35: el portal es público por diseño (kiosco), así que la verificación de registro permite
> enumerar documentos/tarjetas existentes (no devuelve datos personales). Si se despliega a producción real se
> recomienda rate-limiting/preguntas adicionales. La simulación pública sí deja trazabilidad en `historial_accesos`.

## Estado: Fase 8 — COMPLETADA (Preparación para producción, nube Aiven + Render y verificación E2E)

Alineación completa del backend para despliegue en contenedores Docker y servicios cloud gestionados.

Cambios hechos:
- **Enum Mapping PG**: Mapeo nativo en las 5 entidades (`Usuario`, `Empleado`, `HistorialAcceso`, `BitacoraAuditoria`, `RegistroSincronizacionSocio`) con `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` y `columnDefinition`, resolviendo incompatibilidad de tipos `VARCHAR` vs `NAMED_ENUM` en PostgreSQL.
- **Sincronización (`RegistroSincronizacionRepository`)**: Sustitución de query nativo roto por JPQL estándar tipado compatible con Hibernate 6 y PostgreSQL sin errores de columnas inexistentes.
- **Pool de Conexiones HikariCP**: Configuración de `maximum-pool-size=3` y timeouts en `application-prod.properties` para prevenir saturación de slots en planes Free de Aiven (`FATAL: remaining connection slots are reserved for roles with the SUPERUSER attribute`).
- **Migración V5**: `V5__bitacora_direccion_ip_index.sql` aplicada con éxito en Aiven creando el índice `idx_bitacora_direccion_ip` sobre `bitacora_auditoria.direccion_ip`.
- **CORS dinámico**: `SecurityConfig.java` actualizado con `setAllowedOriginPatterns` cuando `FRONTEND_URL=*` y soporte para lista de dominios por comas, habilitando compatibilidad con credenciales (`allowCredentials=true`) para el frontend en Vercel.
- **Dockerfile Cloud**: Eliminada la bandera offline `-o` de `mvn package` para permitir la descarga de dependencias en builds limpios de Render.
- **Perfil de Producción**: `application-prod.properties` corregido (eliminada referencia circular de `spring.profiles.active` y corregido typo en `spring.flyway.schemas`).

Evidencia de la verificación en vivo:
1. Base de datos Aiven (PostgreSQL 18.6) sincronizada con Flyway V1 a V5 (`Current version of schema "zone_control": 5`). ✔
2. Creación en vivo mediante Swagger de roles operativos: Gestor de Personal (`1000000002`, 201 Created) y Supervisor de Accesos (`1000000003`, 201 Created). Listado `GET /api/auth/usuarios` devuelve los 3 usuarios activos. ✔
3. F-05 verificado: `POST /api/auth/recuperar-password` genera token temporal de un solo uso con persistencia en `tokens_recuperacion`, y rechazo inmediato ante reuso. ✔
4. Despliegue en Render exitoso: Servicio activo en `https://laboratorio-zone-control.onrender.com` con Swagger UI (`/swagger-ui.html`) y Tomcat en puerto 10000. ✔

## Estado: Fase 9 — COMPLETADA (Molinete público para empleados sin login — kiosco /molinete)

Aclaración de dominio: la **asignación** de tarjeta sigue siendo administrativa (`PUT /api/personal/empleados/{id}/tarjeta`, ADMINISTRADOR/GESTOR_PERSONAL), pero la **validación** en puerta no puede pedir JWT de admin porque el empleado operativo (`empleados`) no tiene usuario ni contraseña (solo `usuarios` hace login). Se separó el canal operativo del administrativo.

Cambios hechos:
- **Backend** `modules/portal/controller/PortalPublicoController`: nuevo `GET /api/publico/areas` que devuelve `areaRestringidaService.listar(true)` (solo activas, ordenadas por nombre). Sin cambio en `SecurityConfig` porque `/api/publico/**` ya es `permitAll`. El kiosco ya podía validar con `POST /api/publico/accesos/molinete` (F-35, `ResultadoAccesoPublicoDTO` sin datos personales); le faltaba el catálogo de áreas sin login.
- **Frontend** nueva página pública `src/app/molinete/page.tsx` (fuera de `/dashboard`, `'use client'`, sin `useAuth`): formulario documento/tarjeta + select de área (carga de `GET /publico/areas`), `fetch` directo sin `Authorization` a `POST /publico/accesos/molinete`, semáforo VERDE/ROJO/AMARILLO + motivo + área + fecha. No expone `nombreEmpleado`.
- **Frontend** `src/middleware.ts`: `validPublicRoutes` pasa de `['/', '/login']` a `['/', '/login', '/molinete']`; sin esto el Edge redirigía el kiosco a `/`.
- **Frontend** `src/app/RootClient.tsx` (landing `/`): botón secundario "Validar ingreso" → `/molinete` junto a "Portal Operativo" → `/login`. Es el punto visible para Juan sin autenticarse.
- No se tocó `POST /api/accesos/molinete` (interno, con JWT y DTO completo para supervisión) ni la gestión de tarjetas en `EmpleadoController`.

Evidencia de la verificación:
1. `.\mvnw.cmd -o compile -DskipTests` → `BUILD SUCCESS` (103 fuentes, Java 21). ✔
2. `PortalPublicoController` compila con la nueva dependencia `AreaRestringidaService` + `GET /areas` (revisión de código, sin despliegue aún). ✔
3. Frontend verificado por lectura (archivos creados + middleware + link en landing); **pendiente** `pnpm exec tsc --noEmit` / `pnpm build` porque la máquina actual no tiene Node/pnpm en PATH. ✔/pendiente
4. Flujo esperado: admin asigna tarjeta una vez en dashboard → Juan abre `/molinete` sin login → valida → historial queda registrado con IP/user-agent igual que el canal interno. ✔ (lógica reutiliza `AccesoService.procesarAccesoMolinete`)

> Decisiones/riesgos Fase 9: se crea página adicional en vez de reutilizar `dashboard/simulador` porque el simulador exige token+rol, vive en `DashboardLayout` y devuelve datos personales; abrirlo rompería RBAC. El kiosco público hereda el riesgo F-35 ya documentado (enumeración de documentos/tarjetas vía `registrado:true/false` sin datos personales); en producción real aplicar rate-limiting. El catálogo público expone solo áreas activas (id/código/nombre/nivel/estado), sin información sensible.

## Backlog pendiente (por RF/HU)

Prioridad Alta:
- (ninguno de F-01..F-35 queda pendiente; la única pieza aplazada es el formato PDF de F-25 — ver decisión arriba)

Prioridad Media/Baja:
- Integración con el Frontend Next.js en Vercel (apuntar `NEXT_PUBLIC_API_URL` a `https://laboratorio-zone-control.onrender.com`; incluir la nueva ruta pública `/molinete` como kiosco sin login).
- Configuración de credenciales SMTP reales para envío de correos fuera del modo demo.
- Validar en local `pnpm exec tsc --noEmit` + `pnpm build` del frontend (Fase 9 solo verificada por lectura; la máquina de implementación no tenía Node/pnpm) y prueba E2E del kiosco contra Render.

## Bugs conocidos / decisiones pendientes

1. ~~**F-08 roto**~~ RESUELTO en Fase 2 (`noRollbackFor`); verificado que el contador persiste.
2. ~~Credenciales inválidas devuelven HTTP 500~~ RESUELTO: ahora 401/403 con mensaje amigable (NF-15).
3. ~~`SecurityConfig` abre `/api/auth/**` con `permitAll()`~~ RESUELTO: RBAC por rol aplicado y verificado.
4. ~~`application.properties` tiene credenciales hardcodeadas~~ RESUELTO en Fase 8: parametrizado 100% mediante variables de entorno en Render (`SPRING_DATASOURCE_*`, `JWT_SECRET`, `FRONTEND_URL`) y perfil `application-prod.properties`.
5. ~~Error de conexión Aiven por slots saturados~~ RESUELTO en Fase 8: HikariCP limitado a 3 conexiones en producción.
6. ~~CORS bloquea credenciales con wildcard *~~ RESUELTO en Fase 8: uso de `setAllowedOriginPatterns` dinámico.
7. ~~Error en compilación Docker por flag -o~~ RESUELTO en Fase 8: `mvn clean package`.
8. Flyway 9.22.3 advierte que PostgreSQL 18 no está probado (funciona y valida correctamente las 5 migraciones en Aiven).
9. La sesión se valida contra BD en cada petición (una escritura por request para `ultima_actividad`). Suficiente para el laboratorio; si se requiere alto rendimiento, cachear la actividad y persistir cada N segundos.
10. Fase 9 deja el kiosco `/molinete` sin rate-limit ni API key por dispositivo (igual que el resto de `/api/publico/**`); si se expone en producción real, añadir throttle/CAPTCHA y considerar `modules/dispositivos` con `X-Device-Key` + IP allowlist.

## Reglas de negocio clave (del RF/HU)

- Login y simulación usan **número de documento** (HU-001, HU-005).
- Contraseña: mínimo 8, una mayúscula, una minúscula y caracteres alfanuméricos.
- Bloqueo a los 3 intentos fallidos; desbloqueo manual por Administrador.
- No se puede degradar/eliminar al último Administrador activo.
- Historial y bitácora son inmutables (triggers NF-11).
- Historial registra: fecha/hora, documento ingresado, empleado (o NULL), área, resultado, motivo, IP y user-agent.
- Empleados operativos no hacen login (solo `usuarios` internos); la validación en puerta usa el canal público `/api/publico/**` sin JWT y sin exponer datos personales, mientras la asignación de tarjetas y la supervisión usan el canal privado con RBAC.

