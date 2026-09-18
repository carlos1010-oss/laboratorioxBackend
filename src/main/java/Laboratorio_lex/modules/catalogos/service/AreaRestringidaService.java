package Laboratorio_lex.modules.catalogos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.accesos.model.AreaRestringida;
import Laboratorio_lex.modules.accesos.repository.AreaRestringidaRepository;
import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaRequestDTO;
import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaResponseDTO;
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

        return convertirADTO(areaRepository.save(area));
    }

    @Transactional
    public AreaRestringidaResponseDTO actualizar(Integer id, AreaRestringidaRequestDTO dto) {
        AreaRestringida area = buscar(id);

        if (!area.getCodigo().equals(dto.getCodigo()) && areaRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un área restringida con el código: " + dto.getCodigo());
        }

        area.setCodigo(dto.getCodigo());
        area.setNombre(dto.getNombre());
        area.setNivelRiesgo(dto.getNivelRiesgo());
        area.setDescripcion(dto.getDescripcion());

        return convertirADTO(areaRepository.save(area));
    }

    // F-18: activar/desactivar (baja lógica)
    @Transactional
    public AreaRestringidaResponseDTO cambiarEstado(Integer id, Boolean activa) {
        AreaRestringida area = buscar(id);
        area.setActiva(activa);
        return convertirADTO(areaRepository.save(area));
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
}
