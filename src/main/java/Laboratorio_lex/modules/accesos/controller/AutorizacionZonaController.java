package Laboratorio_lex.modules.accesos.controller;

import Laboratorio_lex.modules.accesos.dto.AutorizacionRequestDTO;
import Laboratorio_lex.modules.accesos.dto.AutorizacionResponseDTO;
import Laboratorio_lex.modules.accesos.service.AutorizacionZonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accesos/autorizaciones")
@RequiredArgsConstructor
public class AutorizacionZonaController {

    private final AutorizacionZonaService autorizacionZonaService;

    @GetMapping
    public ResponseEntity<List<AutorizacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(autorizacionZonaService.listarTodas());
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<AutorizacionResponseDTO>> listarPorEmpleado(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(autorizacionZonaService.listarPorEmpleado(empleadoId));
    }

    @PostMapping
    public ResponseEntity<AutorizacionResponseDTO> conceder(@Valid @RequestBody AutorizacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autorizacionZonaService.concederAutorizacion(dto));
    }

    @PatchMapping("/{id}/revocar")
    public ResponseEntity<AutorizacionResponseDTO> revocar(@PathVariable Long id) {
        return ResponseEntity.ok(autorizacionZonaService.revocarAutorizacion(id));
    }
}
