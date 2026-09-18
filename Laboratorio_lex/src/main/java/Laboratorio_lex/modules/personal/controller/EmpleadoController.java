package Laboratorio_lex.modules.personal.controller;

import Laboratorio_lex.modules.personal.dto.AsignarTarjetaDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoResponseDTO;
import Laboratorio_lex.modules.personal.dto.EmpleadoUpdateDTO;
import Laboratorio_lex.modules.personal.dto.ImportacionResultadoDTO;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.service.EmpleadoCsvService;
import Laboratorio_lex.modules.personal.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/personal/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final EmpleadoCsvService empleadoCsvService;

    // F-34: listado paginado y filtrable por documento, nombres, apellidos,
    // departamento y estado
    @GetMapping
    public ResponseEntity<Page<EmpleadoResponseDTO>> listar(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) Integer departamentoId,
            @RequestParam(required = false) EstadoEmpleado estado,
            @PageableDefault(size = 50, sort = "apellidos", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                empleadoService.buscar(documento, nombres, apellidos, departamentoId, estado, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponseDTO> crear(@Valid @RequestBody EmpleadoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crearEmpleado(dto));
    }

    // F-16: modificación de datos (documento inmutable)
    @PutMapping("/{id}")
    public ResponseEntity<EmpleadoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EmpleadoUpdateDTO dto) {
        return ResponseEntity.ok(empleadoService.actualizarEmpleado(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EmpleadoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoEmpleado nuevoEstado,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(empleadoService.cambiarEstado(id, nuevoEstado, motivo));
    }

    // F-19: asignar o reasignar la tarjeta RFID/NFC (simulada por su número)
    @PutMapping("/{id}/tarjeta")
    public ResponseEntity<EmpleadoResponseDTO> asignarTarjeta(
            @PathVariable Long id,
            @Valid @RequestBody AsignarTarjetaDTO dto) {
        return ResponseEntity.ok(empleadoService.asignarTarjeta(id, dto.getCodigoTarjetaRfid()));
    }

    // F-19: desvincular la tarjeta RFID/NFC del empleado
    @DeleteMapping("/{id}/tarjeta")
    public ResponseEntity<EmpleadoResponseDTO> desvincularTarjeta(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.desvincularTarjeta(id));
    }

    // F-14: descargar la plantilla CSV estandarizada
    @GetMapping("/plantilla-csv")
    public ResponseEntity<byte[]> descargarPlantillaCsv() {
        byte[] contenido = empleadoCsvService.generarPlantillaCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_empleados.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    // F-13/F-15: carga masiva desde CSV con validación y reporte de errores
    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResultadoDTO> importarCsv(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(empleadoCsvService.importEmpleadosDesdeCsv(archivo));
    }
}