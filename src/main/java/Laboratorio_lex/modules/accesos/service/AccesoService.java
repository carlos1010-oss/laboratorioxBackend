package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.model.*;
import Laboratorio_lex.modules.accesos.repository.*;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccesoService {

    private final EmpleadoRepository empleadoRepository;
    private final AreaRestringidaRepository areaRestringidaRepository;
    private final AutorizacionZonaRepository autorizacionZonaRepository;
    private final HistorialAccesoRepository historialAccesoRepository;

    @Transactional
    public ResultadoAccesoResponseDTO procesarAccesoMolinete(RegistroAccesoRequestDTO dto, String ipOrigen,
            String userAgent) {

        boolean tieneDocumento = dto.getNumeroDocumento() != null && !dto.getNumeroDocumento().isBlank();
        boolean tieneTarjeta = dto.getCodigoTarjetaRfid() != null && !dto.getCodigoTarjetaRfid().isBlank();

        if (!tieneDocumento && !tieneTarjeta) {
            throw new BadRequestException("Debe ingresar el número de documento o el código de la tarjeta RFID");
        }

        // 1. Validar existencia del área
        Optional<AreaRestringida> areaOpt = areaRestringidaRepository.findById(dto.getAreaId());
        if (areaOpt.isEmpty()) {
            return registrarYResponder(null, null, ResultadoAcceso.NO_REGISTRADO,
                    "Área restringida no encontrada con ID: " + dto.getAreaId(), dto, ipOrigen, userAgent);
        }
        AreaRestringida area = areaOpt.get();

        if (!Boolean.TRUE.equals(area.getActiva())) {
            return registrarYResponder(null, area, ResultadoAcceso.DENEGADO,
                    "El área restringida se encuentra inactiva", dto, ipOrigen, userAgent);
        }

        // 2. Validar existencia del empleado por documento o tarjeta (F-20)
        Optional<Empleado> empleadoOpt = tieneDocumento
                ? empleadoRepository.findByNumeroDocumento(dto.getNumeroDocumento())
                : empleadoRepository.findByCodigoTarjetaRfid(dto.getCodigoTarjetaRfid());

        if (empleadoOpt.isEmpty()) {
            return registrarYResponder(null, area, ResultadoAcceso.NO_REGISTRADO,
                    "Persona no registrada en el sistema", dto, ipOrigen, userAgent);
        }
        Empleado empleado = empleadoOpt.get();

        // 3. Validar estado del empleado
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            return registrarYResponder(empleado, area, ResultadoAcceso.DENEGADO,
                    "Empleado inactivo/suspendido. Estado actual: " + empleado.getEstado(), dto, ipOrigen, userAgent);
        }

        // 4. Validar autorización de zona
        boolean tienePermiso = autorizacionZonaRepository.existsByEmpleadoIdAndAreaIdAndActivoTrue(empleado.getId(),
                area.getId());
        if (!tienePermiso) {
            return registrarYResponder(empleado, area, ResultadoAcceso.DENEGADO,
                    "El empleado no tiene autorización de acceso para esta área restringida", dto, ipOrigen, userAgent);
        }

        // 5. Acceso Autorizado
        return registrarYResponder(empleado, area, ResultadoAcceso.AUTORIZADO,
                "Acceso autorizado", dto, ipOrigen, userAgent);
    }

    // F-24: consulta filtrada y paginada del historial
    @Transactional(readOnly = true)
    public Page<ResultadoAccesoResponseDTO> buscarHistorial(String numeroDocumento, Integer areaId,
            OffsetDateTime fechaInicio, OffsetDateTime fechaFin, Pageable pageable) {
        Specification<HistorialAcceso> spec = HistorialSpecifications.conFiltros(numeroDocumento, areaId, fechaInicio,
                fechaFin);
        return historialAccesoRepository.findAll(spec, pageable).map(this::convertirADTO);
    }

    // F-25: listado completo (sin paginar) de los registros que cumplen los filtros, para exportación
    @Transactional(readOnly = true)
    public List<ResultadoAccesoResponseDTO> listarParaExportar(String numeroDocumento, Integer areaId,
            OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        Specification<HistorialAcceso> spec = HistorialSpecifications.conFiltros(numeroDocumento, areaId, fechaInicio,
                fechaFin);
        return historialAccesoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp")).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private ResultadoAccesoResponseDTO registrarYResponder(Empleado empleado, AreaRestringida area,
            ResultadoAcceso resultado, String motivo, RegistroAccesoRequestDTO dto, String ipOrigen, String userAgent) {
        HistorialAcceso historial = HistorialAcceso.builder()
                .empleado(empleado)
                .area(area)
                .numeroDocumentoIngresado(dto.getNumeroDocumento())
                .codigoTarjetaIngresado(dto.getCodigoTarjetaRfid())
                .resultado(resultado)
                .motivoDenegacion(resultado != ResultadoAcceso.AUTORIZADO ? motivo : null)
                .ipOrigen(toInetAddress(ipOrigen))
                .userAgent(truncar(userAgent, 255))
                .timestamp(OffsetDateTime.now())
                .build();

        HistorialAcceso guardado = historialAccesoRepository.save(historial);
        return convertirADTO(guardado);
    }

    private ResultadoAccesoResponseDTO convertirADTO(HistorialAcceso h) {
        String nombreEmp = (h.getEmpleado() != null)
                ? h.getEmpleado().getNombres() + " " + h.getEmpleado().getApellidos()
                : "DESCONOCIDO";

        String nombreArea = (h.getArea() != null) ? h.getArea().getNombre() : "N/A";

        return ResultadoAccesoResponseDTO.builder()
                .idHistorial(h.getId())
                .numeroDocumentoIngresado(h.getNumeroDocumentoIngresado())
                .codigoTarjetaRfid(h.getCodigoTarjetaIngresado())
                .nombreEmpleado(nombreEmp)
                .nombreArea(nombreArea)
                .resultado(h.getResultado())
                .color(colorDe(h.getResultado()))
                .motivo(h.getMotivoDenegacion())
                .fechaHora(h.getTimestamp())
                .ipOrigen(h.getIpOrigen() != null ? h.getIpOrigen().getHostAddress() : null)
                .userAgent(h.getUserAgent())
                .build();
    }

    // F-22: semáforo visual del resultado
    private String colorDe(ResultadoAcceso resultado) {
        if (resultado == null) {
            return "GRIS";
        }
        return switch (resultado) {
            case AUTORIZADO -> "VERDE";
            case DENEGADO -> "ROJO";
            case NO_REGISTRADO -> "AMARILLO";
        };
    }

    private InetAddress toInetAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
