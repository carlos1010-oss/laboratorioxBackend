package Laboratorio_lex.modules.catalogos.controller;

import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaRequestDTO;
import Laboratorio_lex.modules.catalogos.dto.AreaRestringidaResponseDTO;
import Laboratorio_lex.modules.catalogos.service.AreaRestringidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/catalogos/areas-restringidas", "/api/catalogos/areas"})
@RequiredArgsConstructor
public class AreaRestringidaController {

    private final AreaRestringidaService areaService;

    @GetMapping
    public ResponseEntity<List<AreaRestringidaResponseDTO>> listar(
            @RequestParam(required = false) Boolean soloActivas) {
        return ResponseEntity.ok(areaService.listar(soloActivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaRestringidaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<AreaRestringidaResponseDTO> crear(@Valid @RequestBody AreaRestringidaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaRestringidaResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AreaRestringidaRequestDTO dto) {
        return ResponseEntity.ok(areaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<AreaRestringidaResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Boolean activa) {
        return ResponseEntity.ok(areaService.cambiarEstado(id, activa));
    }
}
