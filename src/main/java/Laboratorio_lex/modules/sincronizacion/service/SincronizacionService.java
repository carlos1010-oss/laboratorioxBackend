package Laboratorio_lex.modules.sincronizacion.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import Laboratorio_lex.modules.sincronizacion.dto.ConsolidadoDepartamentoDTO;
import Laboratorio_lex.modules.sincronizacion.dto.ConsolidadoResponseDTO;
import Laboratorio_lex.modules.sincronizacion.dto.ConfiguracionExportacionDTO;
import Laboratorio_lex.modules.sincronizacion.dto.DashboardSincronizacionDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionFiltroDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionResponseDTO;
import Laboratorio_lex.modules.sincronizacion.model.ConfiguracionExportacion;
import Laboratorio_lex.modules.sincronizacion.model.EstadoSincronizacion;
import Laboratorio_lex.modules.sincronizacion.model.RegistroSincronizacionSocio;
import Laboratorio_lex.modules.sincronizacion.repository.ConfiguracionExportacionRepository;
import Laboratorio_lex.modules.sincronizacion.repository.RegistroSincronizacionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// F-27/F-28: envío POST del consolidado al endpoint del socio con reintentos configurables.
// F-29: alerta por correo en fallo definitivo. F-30: config, estado y reenvío manual.
@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacionService.class);
    private static final Set<String> FORMATOS_VALIDOS = Set.of("JSON");
    private static final Set<String> FRECUENCIAS_VALIDAS = Set.of("DIARIA", "SEMANAL", "MENSUAL");

    private final RegistroSincronizacionRepository sincronizacionRepository;
    private final ConfiguracionExportacionRepository configuracionRepository;
    private final DepartamentoRepository departamentoRepository;
    private final ConsolidadoService consolidadoService;
    private final AlertaCorreoService alertaCorreoService;
    private final ObjectMapper objectMapper;

    @Value("${integracion.socio.simular:true}")
    private boolean simular;

    @Value("${integracion.socio.url:}")
    private String urlSocio;

    @Value("${integracion.socio.max-reintentos:3}")
    private int maxReintentos;

    @Value("${integracion.socio.reintento-minutos:15}")
    private long reintentoMinutos;

    @Transactional
    public SincronizacionResponseDTO generarYEnviarPayload(SincronizacionFiltroDTO filtro) {
        Departamento departamento = null;
        if (filtro.getDepartamentoId() != null) {
            departamento = departamentoRepository.findById(filtro.getDepartamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Departamento no encontrado con ID: " + filtro.getDepartamentoId()));
        }

        String payloadJson = construirPayload(filtro.getPeriodoInicio(), filtro.getPeriodoFin(), departamento);

        RegistroSincronizacionSocio registro = RegistroSincronizacionSocio.builder()
                .periodoInicio(filtro.getPeriodoInicio())
                .periodoFin(filtro.getPeriodoFin())
                .departamento(departamento)
                .payloadJson(payloadJson)
                .estado(EstadoSincronizacion.EN_REINTENTO)
                .intentosRealizados((short) 0)
                .build();

        RegistroSincronizacionSocio guardado = sincronizacionRepository.save(registro);

        procesarEnvioSocio(guardado);

        return convertirADTO(sincronizacionRepository.save(guardado));
    }

    // F-30: reenvío manual de un registro fallido
    @Transactional
    public SincronizacionResponseDTO reenviarManualmente(Long id) {
        RegistroSincronizacionSocio registro = sincronizacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de sincronización no encontrado: " + id));
        if (registro.getEstado() == EstadoSincronizacion.EXITOSO) {
            throw new BadRequestException("El registro ya fue transmitido exitosamente y no requiere reenvío");
        }
        registro.setIntentosRealizados((short) 0);
        registro.setEstado(EstadoSincronizacion.EN_REINTENTO);
        registro.setFechaProximoReintento(null);
        sincronizacionRepository.save(registro);

        procesarEnvioSocio(registro);
        return convertirADTO(sincronizacionRepository.save(registro));
    }

    // F-28: reintentos automáticos cada 5 minutos hasta alcanzar el máximo configurado
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void ejecutarReintentosPendientes() {
        List<RegistroSincronizacionSocio> pendientes = sincronizacionRepository
                .findByEstadoAndFechaProximoReintentoBefore(EstadoSincronizacion.EN_REINTENTO, OffsetDateTime.now());

        for (RegistroSincronizacionSocio reg : pendientes) {
            if (reg.getIntentosRealizados() < maxReintentos) {
                procesarEnvioSocio(reg);
                sincronizacionRepository.save(reg);
            }
        }
    }

    private void procesarEnvioSocio(RegistroSincronizacionSocio reg) {
        boolean exito;
        if (simular) {
            // F-27: modo simulación por defecto mientras el socio no entrega contrato
            log.info("Sincronización SIMULADA registrada como exitosa (ID {}). URL: {}", reg.getId(),
                    resolverUrlSocio());
            exito = true;
        } else {
            exito = enviarHttp(reg.getPayloadJson());
        }

        if (exito) {
            reg.setEstado(EstadoSincronizacion.EXITOSO);
            reg.setCodigoRespuestaHttp((short) 200);
            reg.setFechaEnvio(OffsetDateTime.now());
            reg.setFechaProximoReintento(null);
        } else {
            short intentos = (short) (reg.getIntentosRealizados() + 1);
            reg.setIntentosRealizados(intentos);
            if (intentos >= maxReintentos) {
                reg.setEstado(EstadoSincronizacion.FALLIDO);
                reg.setCodigoRespuestaHttp((short) 503);
                reg.setFechaProximoReintento(null);
                alertaCorreoService.enviarAlertaSupervisor(reg);
            } else {
                reg.setEstado(EstadoSincronizacion.EN_REINTENTO);
                reg.setCodigoRespuestaHttp((short) 500);
                // Reintento programado según la propiedad configurable (por defecto +15 minutos)
                reg.setFechaProximoReintento(OffsetDateTime.now().plusMinutes(reintentoMinutos));
            }
        }
    }

    // F-27: POST JSON real al endpoint del socio
    private boolean enviarHttp(String payloadJson) {
        String url = resolverUrlSocio();
        if (url == null || url.isBlank()) {
            log.warn("No hay URL configurada para el socio (integracion.socio.url o configuracion_exportacion)");
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

            ResponseEntity<String> respuesta = crearRestTemplate().postForEntity(url, entity, String.class);
            boolean exito = respuesta.getStatusCode().is2xxSuccessful();
            if (!exito) {
                log.warn("El socio respondió {} (no-2xx)", respuesta.getStatusCode());
            }
            return exito;
        } catch (Exception e) {
            log.error("Fallo al transmitir el payload al socio: {}", e.getMessage());
            return false;
        }
    }

    private String resolverUrlSocio() {
        if (urlSocio != null && !urlSocio.isBlank()) {
            return urlSocio;
        }
        return configuracionRepository.findFirstByOrderByIdAsc()
                .map(ConfiguracionExportacion::getUrlDestino)
                .orElse(null);
    }

    private RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    private String construirPayload(OffsetDateTime inicio, OffsetDateTime fin, Departamento depto) {
        try {
            ConsolidadoResponseDTO consolidado = consolidadoService.generarConsolidado(inicio, fin);

            List<Map<String, Object>> porDepartamento = consolidado.getPorDepartamento().stream()
                    .filter(d -> depto == null || depto.getId().equals(d.getDepartamentoId()))
                    .map(this::convertirDepartamentoAJson)
                    .collect(Collectors.toList());

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("totalIntentos", consolidado.getTotalIntentos());
            resumen.put("autorizados", consolidado.getTotalAutorizados());
            resumen.put("denegados", consolidado.getTotalDenegados());
            resumen.put("noRegistrados", consolidado.getTotalNoRegistrados());

            Map<String, Object> data = new HashMap<>();
            data.put("tipo", "consolidado_accesos");
            data.put("version", 1);
            data.put("periodoInicio", inicio.toString());
            data.put("periodoFin", fin.toString());
            data.put("departamento", depto != null ? depto.getNombre() : "GLOBAL");
            data.put("resumenGeneral", resumen);
            data.put("porDepartamento", porDepartamento);

            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Fallo al construir el payload: {}", e.getMessage());
            return "{\"error\": \"Fallo al construir payload\"}";
        }
    }

    private Map<String, Object> convertirDepartamentoAJson(ConsolidadoDepartamentoDTO d) {
        Map<String, Object> m = new HashMap<>();
        m.put("departamentoId", d.getDepartamentoId());
        m.put("codigo", d.getCodigo());
        m.put("nombre", d.getNombre());
        m.put("totalIntentos", d.getTotalIntentos());
        m.put("autorizados", d.getAutorizados());
        m.put("denegados", d.getDenegados());
        m.put("noRegistrados", d.getNoRegistrados());
        return m;
    }

    // ---------------- Configuración (F-30) ----------------

    @Transactional(readOnly = true)
    public ConfiguracionExportacionDTO obtenerConfiguracion() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .map(this::convertirConfiguracionADTO)
                .orElse(null);
    }

    @Transactional
    public ConfiguracionExportacionDTO actualizarConfiguracion(ConfiguracionExportacionDTO dto) {
        if (dto.getFrecuencia() != null && !FRECUENCIAS_VALIDAS.contains(dto.getFrecuencia())) {
            throw new BadRequestException("Frecuencia no válida. Use DIARIA, SEMANAL o MENSUAL");
        }
        if (dto.getFormato() != null && !FORMATOS_VALIDOS.contains(dto.getFormato())) {
            throw new BadRequestException("Formato no válido. Actualmente solo se soporta JSON");
        }
        if (Boolean.TRUE.equals(dto.getActivo())
                && (dto.getUrlDestino() == null || dto.getUrlDestino().isBlank())) {
            throw new BadRequestException("Debe configurar la urlDestino antes de activar la integración");
        }

        ConfiguracionExportacion config = configuracionRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Configuración de exportación no inicializada (V2)"));

        config.setFrecuencia(dto.getFrecuencia());
        config.setDiaSemana(dto.getDiaSemana());
        config.setHora(dto.getHora());
        config.setFormato(dto.getFormato());
        config.setUrlDestino(dto.getUrlDestino());
        config.setCorreoAlerta(dto.getCorreoAlerta());
        config.setActivo(dto.getActivo());

        return convertirConfiguracionADTO(configuracionRepository.save(config));
    }

    private ConfiguracionExportacionDTO convertirConfiguracionADTO(ConfiguracionExportacion c) {
        return ConfiguracionExportacionDTO.builder()
                .id(c.getId())
                .frecuencia(c.getFrecuencia())
                .diaSemana(c.getDiaSemana())
                .hora(c.getHora())
                .formato(c.getFormato())
                .urlDestino(c.getUrlDestino())
                .correoAlerta(c.getCorreoAlerta())
                .activo(c.getActivo())
                .fechaProximaEjecucion(c.getFechaProximaEjecucion())
                .ultimaEjecucion(c.getUltimaEjecucion())
                .build();
    }

    // ---------------- Estado para el dashboard (F-30) ----------------

    @Transactional(readOnly = true)
    public DashboardSincronizacionDTO obtenerEstado() {
        ConfiguracionExportacion config = configuracionRepository.findFirstByOrderByIdAsc().orElse(null);

        return DashboardSincronizacionDTO.builder()
                .integracionActiva(config != null && Boolean.TRUE.equals(config.getActivo()))
                .urlWebhook(resolverUrlSocio())
                .correoAlerta(config != null ? config.getCorreoAlerta() : null)
                .modoSimulacion(simular)
                .reintentosMaximos(maxReintentos)
                .ultimaEjecucion(config != null ? config.getUltimaEjecucion() : null)
                .proximaEjecucion(config != null ? config.getFechaProximaEjecucion() : null)
                .pendientes(sincronizacionRepository.countByEstado(EstadoSincronizacion.EN_REINTENTO))
                .exitosos(sincronizacionRepository.countByEstado(EstadoSincronizacion.EXITOSO))
                .fallidos(sincronizacionRepository.countByEstado(EstadoSincronizacion.FALLIDO))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SincronizacionResponseDTO> listarHistorial() {
        return sincronizacionRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private SincronizacionResponseDTO convertirADTO(RegistroSincronizacionSocio r) {
        return SincronizacionResponseDTO.builder()
                .id(r.getId())
                .periodoInicio(r.getPeriodoInicio())
                .periodoFin(r.getPeriodoFin())
                .departamentoId(r.getDepartamento() != null ? r.getDepartamento().getId() : null)
                .nombreDepartamento(r.getDepartamento() != null ? r.getDepartamento().getNombre() : "GLOBAL")
                .payloadJson(r.getPayloadJson())
                .estado(r.getEstado())
                .intentosRealizados(r.getIntentosRealizados())
                .codigoRespuestaHttp(r.getCodigoRespuestaHttp())
                .fechaEnvio(r.getFechaEnvio())
                .fechaProximoReintento(r.getFechaProximoReintento())
                .createdAt(r.getCreatedAt())
                .build();
    }
}