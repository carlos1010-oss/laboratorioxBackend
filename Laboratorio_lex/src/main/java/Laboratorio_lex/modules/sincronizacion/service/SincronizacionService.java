package Laboratorio_lex.modules.sincronizacion.service;

import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.accesos.repository.HistorialAccesoRepository;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionFiltroDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionResponseDTO;
import Laboratorio_lex.modules.sincronizacion.model.EstadoSincronizacion;
import Laboratorio_lex.modules.sincronizacion.model.RegistroSincronizacionSocio;
import Laboratorio_lex.modules.sincronizacion.repository.RegistroSincronizacionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private final RegistroSincronizacionRepository sincronizacionRepository;
    private final DepartamentoRepository departamentoRepository;
    private final HistorialAccesoRepository historialAccesoRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SincronizacionResponseDTO generarYEnviarPayload(SincronizacionFiltroDTO filtro) {
        Departamento departamento = null;
        if (filtro.getDepartamentoId() != null) {
            departamento = departamentoRepository.findById(filtro.getDepartamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Departamento no encontrado con ID: " + filtro.getDepartamentoId()));
        }

        // 1. Construir el payload JSON con los registros del periodo
        String payloadJson = construirPayload(filtro.getPeriodoInicio(), filtro.getPeriodoFin(), departamento);

        // 2. Crear el registro de sincronización
        RegistroSincronizacionSocio registro = RegistroSincronizacionSocio.builder()
                .periodoInicio(filtro.getPeriodoInicio())
                .periodoFin(filtro.getPeriodoFin())
                .departamento(departamento)
                .payloadJson(payloadJson)
                .estado(EstadoSincronizacion.EN_REINTENTO)
                .intentosRealizados((short) 0)
                .build();

        RegistroSincronizacionSocio guardado = sincronizacionRepository.save(registro);

        // 3. Simulación de transmisión al endpoint del socio
        procesarEnvioSocio(guardado);

        return convertirADTO(sincronizacionRepository.save(guardado));
    }

    @Scheduled(fixedRate = 300000) // Se ejecuta cada 5 minutos
    @Transactional
    public void ejecutarReintentosPendientes() {
        List<RegistroSincronizacionSocio> pendientes = sincronizacionRepository
                .findByEstadoAndFechaProximoReintentoBefore(EstadoSincronizacion.EN_REINTENTO, OffsetDateTime.now());

        for (RegistroSincronizacionSocio reg : pendientes) {
            if (reg.getIntentosRealizados() < 3) {
                procesarEnvioSocio(reg);
                sincronizacionRepository.save(reg);
            }
        }
    }

    private void procesarEnvioSocio(RegistroSincronizacionSocio reg) {
        short intentos = (short) (reg.getIntentosRealizados() + 1);
        reg.setIntentosRealizados(intentos);

        // Simulación: Transmisión HTTP exitosa (200 OK)
        boolean exitoSimulado = true;

        if (exitoSimulado) {
            reg.setEstado(EstadoSincronizacion.EXITOSO);
            reg.setCodigoRespuestaHttp((short) 200);
            reg.setFechaEnvio(OffsetDateTime.now());
            reg.setFechaProximoReintento(null);
        } else {
            if (intentos >= 3) {
                reg.setEstado(EstadoSincronizacion.FALLIDO);
                reg.setCodigoRespuestaHttp((short) 503);
                reg.setFechaProximoReintento(null);
            } else {
                reg.setEstado(EstadoSincronizacion.EN_REINTENTO);
                reg.setCodigoRespuestaHttp((short) 500);
                // Reintento programado en +15 minutos
                reg.setFechaProximoReintento(OffsetDateTime.now().plusMinutes(15));
            }
        }
    }

    private String construirPayload(OffsetDateTime inicio, OffsetDateTime fin, Departamento depto) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("periodoInicio", inicio.toString());
            data.put("periodoFin", fin.toString());
            data.put("departamento", depto != null ? depto.getNombre() : "GLOBAL");
            data.put("totalAccesosEvaluados", historialAccesoRepository.count());

            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\": \"Fallo al construir payload: " + e.getMessage() + "\"}";
        }
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