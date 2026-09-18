package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.accesos.dto.AutorizacionRequestDTO;
import Laboratorio_lex.modules.accesos.dto.AutorizacionResponseDTO;
import Laboratorio_lex.modules.accesos.model.AreaRestringida;
import Laboratorio_lex.modules.accesos.model.AutorizacionZona;
import Laboratorio_lex.modules.accesos.repository.AreaRestringidaRepository;
import Laboratorio_lex.modules.accesos.repository.AutorizacionZonaRepository;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auditoria.util.AuditoriaContexto;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutorizacionZonaService {

    private final AutorizacionZonaRepository autorizacionZonaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AreaRestringidaRepository areaRestringidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final AuditoriaContexto auditoriaContexto;
    private final ObjectMapper objectMapper;

    @Transactional
    public AutorizacionResponseDTO concederAutorizacion(AutorizacionRequestDTO dto) {
        Empleado empleado = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Empleado no encontrado con ID: " + dto.getEmpleadoId()));

        AreaRestringida area = areaRestringidaRepository.findById(dto.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Área restringida no encontrada con ID: " + dto.getAreaId()));

        Usuario asignadoPor = usuarioRepository.findById(dto.getAsignadoPorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario asignador no encontrado con ID: " + dto.getAsignadoPorId()));

        Optional<AutorizacionZona> existOpt = autorizacionZonaRepository.findByEmpleadoIdAndAreaId(empleado.getId(),
                area.getId());

        AutorizacionZona autorizacion;
        if (existOpt.isPresent()) {
            autorizacion = existOpt.get();
            if (Boolean.TRUE.equals(autorizacion.getActivo())) {
                throw new BadRequestException("El empleado ya cuenta con una autorización activa para esta área");
            }
            autorizacion.setActivo(true);
            autorizacion.setAsignadoPor(asignadoPor);
        } else {
            autorizacion = AutorizacionZona.builder()
                    .empleado(empleado)
                    .area(area)
                    .asignadoPor(asignadoPor)
                    .activo(true)
                    .build();
        }

        AutorizacionZona guardada = autorizacionZonaRepository.save(autorizacion);
        AutorizacionResponseDTO response = convertirADTO(guardada);

        registrarAuditoria(null, response, TipoOperacion.CREACION);

        return response;
    }

    @Transactional
    public AutorizacionResponseDTO revocarAutorizacion(Long id) {
        AutorizacionZona autorizacion = autorizacionZonaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autorización de zona no encontrada con ID: " + id));

        AutorizacionResponseDTO estadoAnterior = convertirADTO(autorizacion);

        autorizacion.setActivo(false);

        AutorizacionZona actualizada = autorizacionZonaRepository.save(autorizacion);
        AutorizacionResponseDTO estadoNuevo = convertirADTO(actualizada);

        registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

        return estadoNuevo;
    }

    @Transactional(readOnly = true)
    public List<AutorizacionResponseDTO> listarPorEmpleado(Long empleadoId) {
        return autorizacionZonaRepository.findByEmpleadoId(empleadoId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AutorizacionResponseDTO> listarTodas() {
        return autorizacionZonaRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private void registrarAuditoria(Object anterior, Object nuevo, TipoOperacion operacion) {
        try {
            String jsonAnterior = anterior != null ? objectMapper.writeValueAsString(anterior) : null;
            String jsonNuevo = nuevo != null ? objectMapper.writeValueAsString(nuevo) : null;

            Usuario usuarioActual = auditoriaContexto.obtenerUsuarioActual();

            AuditoriaRequestDTO auditoriaDTO = AuditoriaRequestDTO.builder()
                    .usuarioId(usuarioActual != null ? usuarioActual.getId() : null)
                    .direccionIp(auditoriaContexto.obtenerIpActual())
                    .tipoOperacion(operacion)
                    .moduloTabla("autorizaciones_zona")
                    .valorAnterior(jsonAnterior)
                    .valorNuevo(jsonNuevo)
                    .build();

            auditoriaService.registrarEvento(auditoriaDTO);
        } catch (Exception e) {
            System.err.println("Error al registrar evento de auditoría en autorizaciones_zona: " + e.getMessage());
        }
    }

    private AutorizacionResponseDTO convertirADTO(AutorizacionZona a) {
        String nombreEmp = (a.getEmpleado() != null)
                ? a.getEmpleado().getNombres() + " " + a.getEmpleado().getApellidos()
                : "DESCONOCIDO";

        String nombreArea = (a.getArea() != null) ? a.getArea().getNombre() : "N/A";

        String asignador = (a.getAsignadoPor() != null)
                ? a.getAsignadoPor().getNombres() + " " + a.getAsignadoPor().getApellidos()
                : "SISTEMA";

        return AutorizacionResponseDTO.builder()
                .id(a.getId())
                .empleadoId(a.getEmpleado() != null ? a.getEmpleado().getId() : null)
                .nombreEmpleado(nombreEmp)
                .areaId(a.getArea() != null ? a.getArea().getId() : null)
                .nombreArea(nombreArea)
                .asignadoPorUsuario(asignador)
                .activo(a.getActivo())
                .fechaAsignacion(a.getFechaAsignacion())
                .build();
    }
}