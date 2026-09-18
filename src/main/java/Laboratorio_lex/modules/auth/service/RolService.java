package Laboratorio_lex.modules.auth.service;

import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.auth.dto.RolResponseDTO;
import Laboratorio_lex.modules.auth.model.Rol;
import Laboratorio_lex.modules.auth.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolResponseDTO obtenerPorId(Integer id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));
        return convertirADTO(rol);
    }

    private RolResponseDTO convertirADTO(Rol rol) {
        return RolResponseDTO.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .createdAt(rol.getCreatedAt())
                .build();
    }
}
