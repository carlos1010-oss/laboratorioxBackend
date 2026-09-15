package Laboratorio_lex.modules.accesos.controller;

import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.service.AccesoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accesos")
@RequiredArgsConstructor
public class AccesoController {

    private final AccesoService accesoService;

    @PostMapping("/molinete")
    public ResponseEntity<ResultadoAccesoResponseDTO> registrarAccesoMolinete(
            @Valid @RequestBody RegistroAccesoRequestDTO dto) {
        return ResponseEntity.ok(accesoService.procesarAccesoMolinete(dto));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ResultadoAccesoResponseDTO>> obtenerHistorial() {
        return ResponseEntity.ok(accesoService.obtenerHistorial());
    }
}