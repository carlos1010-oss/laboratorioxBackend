package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.model.*;
import Laboratorio_lex.modules.accesos.repository.*;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ResultadoAccesoResponseDTO procesarAccesoMolinete(RegistroAccesoRequestDTO dto) {

        // 1. Validar existencia del área
        Optional<AreaRestringida> areaOpt = areaRestringidaRepository.findById(dto.getAreaId());
        if (areaOpt.isEmpty()) {
            return registrarYResponder(null, null, ResultadoAcceso.NO_REGISTRADO,
                    "Área restringida no encontrada con ID: " + dto.getAreaId(), dto.getCodigoTarjetaRfid());
        }
        AreaRestringida area = areaOpt.get();

        if (!Boolean.TRUE.equals(area.getActiva())) {
            return registrarYResponder(null, area, ResultadoAcceso.DENEGADO,
                    "El área restringida se encuentra inactiva", dto.getCodigoTarjetaRfid());
        }

        // 2. Validar existencia de la tarjeta RFID
        Optional<Empleado> empleadoOpt = empleadoRepository.findByCodigoTarjetaRfid(dto.getCodigoTarjetaRfid());
        if (empleadoOpt.isEmpty()) {
            return registrarYResponder(null, area, ResultadoAcceso.NO_REGISTRADO,
                    "Tarjeta RFID no vinculada a ningún empleado en el sistema", dto.getCodigoTarjetaRfid());
        }
        Empleado empleado = empleadoOpt.get();

        // 3. Validar estado del empleado
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            return registrarYResponder(empleado, area, ResultadoAcceso.DENEGADO,
                    "Empleado inactivo/suspendido. Estado actual: " + empleado.getEstado(), dto.getCodigoTarjetaRfid());
        }

        // 4. Validar autorización de zona
        boolean tienePermiso = autorizacionZonaRepository.existsByEmpleadoIdAndAreaIdAndActivoTrue(empleado.getId(),
                area.getId());
        if (!tienePermiso) {
            return registrarYResponder(empleado, area, ResultadoAcceso.DENEGADO,
                    "El empleado no tiene autorización de acceso para esta área biosegura", dto.getCodigoTarjetaRfid());
        }

        // 5. Acceso Autorizado
        return registrarYResponder(empleado, area, ResultadoAcceso.AUTORIZADO,
                "Acceso autorizado", dto.getCodigoTarjetaRfid());
    }

    @Transactional(readOnly = true)
    public List<ResultadoAccesoResponseDTO> obtenerHistorial() {
        return historialAccesoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private ResultadoAccesoResponseDTO registrarYResponder(Empleado empleado, AreaRestringida area,
            ResultadoAcceso resultado, String motivo, String rfid) {
        HistorialAcceso historial = HistorialAcceso.builder()
                .empleado(empleado)
                .area(area)
                .codigoTarjetaPresentada(rfid)
                .resultado(resultado)
                .motivoDenegacion(resultado != ResultadoAcceso.AUTORIZADO ? motivo : null)
                .fechaHora(OffsetDateTime.now())
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
                .codigoTarjetaRfid(h.getCodigoTarjetaPresentada())
                .nombreEmpleado(nombreEmp)
                .nombreArea(nombreArea)
                .resultado(h.getResultado())
                .motivo(h.getMotivoDenegacion())
                .fechaHora(h.getFechaHora())
                .build();
    }
}