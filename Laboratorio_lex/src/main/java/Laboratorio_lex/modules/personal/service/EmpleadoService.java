package Laboratorio_lex.modules.personal.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoResponseDTO;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final DepartamentoRepository departamentoRepository;

    @Transactional(readOnly = true)
    public List<EmpleadoResponseDTO> obtenerTodos() {
        return empleadoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpleadoResponseDTO obtenerPorId(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));
        return convertirADTO(empleado);
    }

    @Transactional
    public EmpleadoResponseDTO crearEmpleado(EmpleadoRequestDTO dto) {
        if (empleadoRepository.existsByNumeroDocumento(dto.getNumeroDocumento())) {
            throw new BadRequestException(
                    "Ya existe un empleado registrado con el documento: " + dto.getNumeroDocumento());
        }

        if (dto.getCodigoTarjetaRfid() != null
                && empleadoRepository.existsByCodigoTarjetaRfid(dto.getCodigoTarjetaRfid())) {
            throw new BadRequestException(
                    "La tarjeta RFID ya está asignada a otro empleado: " + dto.getCodigoTarjetaRfid());
        }

        Departamento departamento = departamentoRepository.findById(dto.getDepartamentoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Departamento no encontrado con ID: " + dto.getDepartamentoId()));

        Empleado empleado = Empleado.builder()
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(dto.getNumeroDocumento())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .departamento(departamento)
                .codigoTarjetaRfid(dto.getCodigoTarjetaRfid())
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        Empleado guardado = empleadoRepository.save(empleado);
        return convertirADTO(guardado);
    }

    @Transactional
    public EmpleadoResponseDTO cambiarEstado(Long id, EstadoEmpleado nuevoEstado, String motivo) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));

        empleado.setEstado(nuevoEstado);
        empleado.setMotivoCambioEstado(motivo);

        Empleado actualizado = empleadoRepository.save(empleado);
        return convertirADTO(actualizado);
    }

    private EmpleadoResponseDTO convertirADTO(Empleado empleado) {
        return EmpleadoResponseDTO.builder()
                .id(empleado.getId())
                .tipoDocumento(empleado.getTipoDocumento())
                .numeroDocumento(empleado.getNumeroDocumento())
                .nombres(empleado.getNombres())
                .apellidos(empleado.getApellidos())
                .correo(empleado.getCorreo())
                .telefono(empleado.getTelefono())
                .departamentoId(empleado.getDepartamento() != null ? empleado.getDepartamento().getId() : null)
                .departamentoNombre(empleado.getDepartamento() != null ? empleado.getDepartamento().getNombre() : null)
                .codigoTarjetaRfid(empleado.getCodigoTarjetaRfid())
                .estado(empleado.getEstado() != null ? empleado.getEstado().name() : null)
                .motivoCambioEstado(empleado.getMotivoCambioEstado())
                .createdAt(empleado.getCreatedAt())
                .build();
    }
}