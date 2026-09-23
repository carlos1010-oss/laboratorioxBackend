package Laboratorio_lex.modules.personal.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auditoria.util.AuditoriaContexto;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoResponseDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoUpdateDTO;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import Laboratorio_lex.modules.personal.repository.EmpleadoSpecifications;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

        private final EmpleadoRepository empleadoRepository;
        private final DepartamentoRepository departamentoRepository;
        private final AuditoriaService auditoriaService;
        private final AuditoriaContexto auditoriaContexto;
        private final ObjectMapper objectMapper;

        // F-34: búsqueda paginada y filtrable por documento, nombres, apellidos,
        // departamento y estado
        @Transactional(readOnly = true)
        public Page<EmpleadoResponseDTO> buscar(
                        String documento,
                        String nombres,
                        String apellidos,
                        Integer departamentoId,
                        EstadoEmpleado estado,
                        Pageable pageable) {

                return empleadoRepository
                                .findAll(EmpleadoSpecifications.conFiltros(
                                                documento, nombres, apellidos, departamentoId, estado), pageable)
                                .map(this::convertirADTO);
        }

        @Transactional(readOnly = true)
        public EmpleadoResponseDTO obtenerPorId(Long id) {
                Empleado empleado = empleadoRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Empleado no encontrado con ID: " + id));
                return convertirADTO(empleado);
        }

        @Transactional
        public EmpleadoResponseDTO crearEmpleado(EmpleadoRequestDTO dto) {
                if (empleadoRepository.existsByNumeroDocumento(dto.getNumeroDocumento())) {
                        throw new BadRequestException("Ya existe un empleado registrado con el documento: "
                                        + dto.getNumeroDocumento());
                }

                if (dto.getCodigoTarjetaRfid() != null
                                && empleadoRepository.existsByCodigoTarjetaRfid(dto.getCodigoTarjetaRfid())) {
                        throw new BadRequestException("La tarjeta RFID ya está asignada a otro empleado: "
                                        + dto.getCodigoTarjetaRfid());
                }

                Departamento departamento = departamentoRepository.findById(dto.getDepartamentoId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Departamento no encontrado con ID: " + dto.getDepartamentoId()));

                // F-17: si el estado no es ACTIVO, el motivo es obligatorio
                EstadoEmpleado estado = dto.getEstado() != null ? dto.getEstado() : EstadoEmpleado.ACTIVO;
                validarMotivoEstado(estado, dto.getMotivoCambioEstado());

                Empleado empleado = Empleado.builder()
                                .tipoDocumento(dto.getTipoDocumento())
                                .numeroDocumento(dto.getNumeroDocumento())
                                .nombres(dto.getNombres())
                                .apellidos(dto.getApellidos())
                                .correo(dto.getCorreo())
                                .telefono(dto.getTelefono())
                                .departamento(departamento)
                                .codigoTarjetaRfid(dto.getCodigoTarjetaRfid())
                                .estado(estado)
                                .motivoCambioEstado(estado == EstadoEmpleado.ACTIVO ? null : dto.getMotivoCambioEstado())
                                .build();

                Empleado guardado = empleadoRepository.save(empleado);
                EmpleadoResponseDTO response = convertirADTO(guardado);

                // Registrar evento de auditoría (CREACIÓN)
                registrarAuditoria(null, response, TipoOperacion.CREACION);

                return response;
        }

        // F-16: modifica los datos del empleado; el número de documento es inmutable
        @Transactional
        public EmpleadoResponseDTO actualizarEmpleado(Long id, EmpleadoUpdateDTO dto) {
                Empleado empleado = empleadoRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Empleado no encontrado con ID: " + id));

                // F-16: rechazar cualquier intento de cambiar el documento
                if (dto.getNumeroDocumento() != null && !dto.getNumeroDocumento().isBlank()
                                && !dto.getNumeroDocumento().equals(empleado.getNumeroDocumento())) {
                        throw new BadRequestException(
                                        "El número de documento es inmutable y no puede modificarse (F-16).");
                }

                Departamento departamento = departamentoRepository.findById(dto.getDepartamentoId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Departamento no encontrado con ID: " + dto.getDepartamentoId()));

                EmpleadoResponseDTO estadoAnterior = convertirADTO(empleado);

                empleado.setTipoDocumento(dto.getTipoDocumento());
                empleado.setNombres(dto.getNombres());
                empleado.setApellidos(dto.getApellidos());
                empleado.setCorreo(dto.getCorreo());
                empleado.setTelefono(dto.getTelefono());
                empleado.setDepartamento(departamento);

                Empleado actualizado = empleadoRepository.save(empleado);
                EmpleadoResponseDTO estadoNuevo = convertirADTO(actualizado);

                registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

                return estadoNuevo;
        }

        // F-19: asigna o reasigna una credencial RFID/NFC (simulada por su número)
        @Transactional
        public EmpleadoResponseDTO asignarTarjeta(Long id, String codigoTarjeta) {
                Empleado empleado = empleadoRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Empleado no encontrado con ID: " + id));

                if (codigoTarjeta == null || codigoTarjeta.isBlank()) {
                        throw new BadRequestException("El código de la tarjeta RFID/NFC es obligatorio (F-19).");
                }

                empleadoRepository.findByCodigoTarjetaRfidIgnoreCase(codigoTarjeta).ifPresent(otro -> {
                        if (!otro.getId().equals(id)) {
                                throw new BadRequestException(
                                                "La tarjeta RFID/NFC ya está asignada a otro empleado: " + codigoTarjeta);
                        }
                });

                EmpleadoResponseDTO estadoAnterior = convertirADTO(empleado);
                empleado.setCodigoTarjetaRfid(codigoTarjeta);
                EmpleadoResponseDTO estadoNuevo = convertirADTO(empleadoRepository.save(empleado));

                registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

                return estadoNuevo;
        }

        // F-19: desvincula la credencial RFID/NFC del empleado
        @Transactional
        public EmpleadoResponseDTO desvincularTarjeta(Long id) {
                Empleado empleado = empleadoRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Empleado no encontrado con ID: " + id));

                if (empleado.getCodigoTarjetaRfid() == null) {
                        throw new BadRequestException("El empleado no tiene una tarjeta RFID/NFC asignada.");
                }

                EmpleadoResponseDTO estadoAnterior = convertirADTO(empleado);
                empleado.setCodigoTarjetaRfid(null);
                EmpleadoResponseDTO estadoNuevo = convertirADTO(empleadoRepository.save(empleado));

                registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

                return estadoNuevo;
        }

        @Transactional
        public EmpleadoResponseDTO cambiarEstado(Long id, EstadoEmpleado nuevoEstado, String motivo) {
                Empleado empleado = empleadoRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Empleado no encontrado con ID: " + id));

                // F-17: motivo obligatorio al inactivar o bloquear
                validarMotivoEstado(nuevoEstado, motivo);

                EmpleadoResponseDTO estadoAnterior = convertirADTO(empleado);

                empleado.setEstado(nuevoEstado);
                empleado.setMotivoCambioEstado(nuevoEstado == EstadoEmpleado.ACTIVO ? null : motivo);

                Empleado actualizado = empleadoRepository.save(empleado);
                EmpleadoResponseDTO estadoNuevo = convertirADTO(actualizado);

                // Determinar tipo de operación para la bitácora (BLOQUEO / MODIFICACION)
                TipoOperacion operacion = (nuevoEstado == EstadoEmpleado.INACTIVO)
                                ? TipoOperacion.BLOQUEO
                                : TipoOperacion.MODIFICACION;

                // Registrar evento de auditoría
                registrarAuditoria(estadoAnterior, estadoNuevo, operacion);

                return estadoNuevo;
        }

        // F-17: valida que el motivo esté presente cuando el estado no es ACTIVO
        private void validarMotivoEstado(EstadoEmpleado estado, String motivo) {
                if (estado != null && estado != EstadoEmpleado.ACTIVO
                                && (motivo == null || motivo.isBlank())) {
                        throw new BadRequestException(
                                        "El motivo de cambio de estado es obligatorio cuando el estado es INACTIVO (F-17).");
                }
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
                                        .moduloTabla("empleados")
                                        .valorAnterior(jsonAnterior)
                                        .valorNuevo(jsonNuevo)
                                        .build();

                        auditoriaService.registrarEvento(auditoriaDTO);
                } catch (Exception e) {
                        // Se loguea la excepción sin interrumpir la transacción principal de empleados
                        System.err.println("Error al registrar evento de auditoría: " + e.getMessage());
                }
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
                                .departamentoId(empleado.getDepartamento() != null ? empleado.getDepartamento().getId()
                                                : null)
                                .departamentoNombre(empleado.getDepartamento() != null
                                                ? empleado.getDepartamento().getNombre()
                                                : null)
                                .codigoTarjetaRfid(empleado.getCodigoTarjetaRfid())
                                .estado(empleado.getEstado() != null ? empleado.getEstado().name() : null)
                                .motivoCambioEstado(empleado.getMotivoCambioEstado())
                                .createdAt(empleado.getCreatedAt())
                                .build();
        }
}
