package Laboratorio_lex.modules.personal.controller;

import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoResponseDTO;
import Laboratorio_lex.modules.personal.dto.ImportacionResultadoDTO;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.service.EmpleadoCsvService;
import Laboratorio_lex.modules.personal.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/personal/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final EmpleadoCsvService empleadoCsvService; // <--- Inyección del nuevo servicio

    @GetMapping
    public ResponseEntity<List<EmpleadoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(empleadoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponseDTO> crear(@Valid @RequestBody EmpleadoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crearEmpleado(dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EmpleadoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoEmpleado nuevoEstado,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(empleadoService.cambiarEstado(id, nuevoEstado, motivo));
    }

    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResultadoDTO> importarCsv(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(empleadoCsvService.importEmpleadosDesdeCsv(archivo));
    }
}