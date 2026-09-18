package Laboratorio_lex.modules.catalogos.controller;

import Laboratorio_lex.modules.catalogos.dto.DepartamentoRequestDTO;
import Laboratorio_lex.modules.catalogos.dto.DepartamentoResponseDTO;
import Laboratorio_lex.modules.catalogos.service.DepartamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/departamentos")
@RequiredArgsConstructor
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    @GetMapping
    public ResponseEntity<List<DepartamentoResponseDTO>> listar(
            @RequestParam(required = false) Boolean soloActivos) {
        return ResponseEntity.ok(departamentoService.listar(soloActivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartamentoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(departamentoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<DepartamentoResponseDTO> crear(@Valid @RequestBody DepartamentoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departamentoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartamentoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody DepartamentoRequestDTO dto) {
        return ResponseEntity.ok(departamentoService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<DepartamentoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Boolean activo) {
        return ResponseEntity.ok(departamentoService.cambiarEstado(id, activo));
    }
}
