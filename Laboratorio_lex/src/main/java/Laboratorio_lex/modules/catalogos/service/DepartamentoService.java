package Laboratorio_lex.modules.catalogos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.catalogos.dto.DepartamentoRequestDTO;
import Laboratorio_lex.modules.catalogos.dto.DepartamentoResponseDTO;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;

    @Transactional(readOnly = true)
    public List<DepartamentoResponseDTO> listar(Boolean soloActivos) {
        return departamentoRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(soloActivos) || Boolean.TRUE.equals(d.getActivo()))
                .sorted(Comparator.comparing(Departamento::getNombre))
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartamentoResponseDTO obtenerPorId(Integer id) {
        return convertirADTO(buscar(id));
    }

    @Transactional
    public DepartamentoResponseDTO crear(DepartamentoRequestDTO dto) {
        if (departamentoRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un departamento con el código: " + dto.getCodigo());
        }

        Departamento departamento = Departamento.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .activo(true)
                .build();

        return convertirADTO(departamentoRepository.save(departamento));
    }

    @Transactional
    public DepartamentoResponseDTO actualizar(Integer id, DepartamentoRequestDTO dto) {
        Departamento departamento = buscar(id);

        if (!departamento.getCodigo().equals(dto.getCodigo())
                && departamentoRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un departamento con el código: " + dto.getCodigo());
        }

        departamento.setCodigo(dto.getCodigo());
        departamento.setNombre(dto.getNombre());
        departamento.setDescripcion(dto.getDescripcion());

        return convertirADTO(departamentoRepository.save(departamento));
    }

    // F-18: activar/desactivar (baja lógica)
    @Transactional
    public DepartamentoResponseDTO cambiarEstado(Integer id, Boolean activo) {
        Departamento departamento = buscar(id);
        departamento.setActivo(activo);
        return convertirADTO(departamentoRepository.save(departamento));
    }

    private Departamento buscar(Integer id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado con ID: " + id));
    }

    private DepartamentoResponseDTO convertirADTO(Departamento d) {
        return DepartamentoResponseDTO.builder()
                .id(d.getId())
                .codigo(d.getCodigo())
                .nombre(d.getNombre())
                .descripcion(d.getDescripcion())
                .activo(d.getActivo())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
