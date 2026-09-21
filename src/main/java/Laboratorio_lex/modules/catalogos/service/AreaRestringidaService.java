package Laboratorio_lex.modules.catalogos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.accesos.model.AreaRestringida;
import Laboratorio_lex.modules.accesos.repository.AreaRestringidaRepository;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auditoria.util.AuditoriaContexto;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaRequestDTO;
import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaRestringidaService {

    private final AreaRestringidaRepository areaRepository;
    private final AuditoriaService auditoriaService;
    private final AuditoriaContexto auditoriaContexto;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<AreaRestringidaResponseDTO> listar(Boolean soloActivas) {
        return areaRepository.findAll().stream()
                .filter(a -> !Boolean.TRUE.equals(soloActivas) || Boolean.TRUE.equals(a.getActiva()))
                .sorted(Comparator.comparing(AreaRestringida::getNombre))
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AreaRestringidaResponseDTO obtenerPorId(Integer id) {
        return convertirADTO(buscar(id));
    }

    @Transactional
    public AreaRestringidaResponseDTO crear(AreaRestringidaRequestDTO dto) {
        if (areaRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un área restringida con el código: " + dto.getCodigo());
        }

        AreaRestringida area = AreaRestringida.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .nivelRiesgo(dto.getNivelRiesgo())
                .descripcion(dto.getDescripcion())
                .activa(true)
                .build();

        AreaRestringidaResponseDTO response = convertirADTO(areaRepository.save(area));
        registrarAuditoria(null, response, TipoOperacion.CREACION);
        return response;
    }

    @Transactional
    public AreaRestringidaResponseDTO actualizar(Integer id, AreaRestringidaRequestDTO dto) {
        AreaRestringida area = buscar(id);
        AreaRestringidaResponseDTO estadoAnterior = convertirADTO(area);

        if (!area.getCodigo().equals(dto.getCodigo()) && areaRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un área restringida con el código: " + dto.getCodigo());
        }

        area.setCodigo(dto.getCodigo());
        area.setNombre(dto.getNombre());
        area.setNivelRiesgo(dto.getNivelRiesgo());
        area.setDescripcion(dto.getDescripcion());

        AreaRestringidaResponseDTO response = convertirADTO(areaRepository.save(area));
        registrarAuditoria(estadoAnterior, response, TipoOperacion.MODIFICACION);
        return response;
    }

    // F-18: activar/desactivar (baja lógica)
    @Transactional
    public AreaRestringidaResponseDTO cambiarEstado(Integer id, Boolean activa) {
        AreaRestringida area = buscar(id);
        AreaRestringidaResponseDTO estadoAnterior = convertirADTO(area);

        area.setActiva(activa);

        AreaRestringidaResponseDTO response = convertirADTO(areaRepository.save(area));
        TipoOperacion operacion = Boolean.TRUE.equals(activa) ? TipoOperacion.MODIFICACION : TipoOperacion.ELIMINACION_LOGICA;
        registrarAuditoria(estadoAnterior, response, operacion);
        return response;
    }

    private AreaRestringida buscar(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área restringida no encontrada con ID: " + id));
    }

    private AreaRestringidaResponseDTO convertirADTO(AreaRestringida a) {
        return AreaRestringidaResponseDTO.builder()
                .id(a.getId())
                .codigo(a.getCodigo())
                .nombre(a.getNombre())
                .nivelRiesgo(a.getNivelRiesgo())
                .descripcion(a.getDescripcion())
                .activa(a.getActiva())
                .createdAt(a.getCreatedAt())
                .build();
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
                    .moduloTabla("areas_restringidas")
                    .valorAnterior(jsonAnterior)
                    .valorNuevo(jsonNuevo)
                    .build();

            auditoriaService.registrarEvento(auditoriaDTO);
        } catch (Exception e) {
            System.err.println("Error al registrar evento de auditoría en areas_restringidas: " + e.getMessage());
        }
    }
}
