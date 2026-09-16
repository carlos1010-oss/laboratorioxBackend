package Laboratorio_lex.modules.sincronizacion.controller;

import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionFiltroDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionResponseDTO;
import Laboratorio_lex.modules.sincronizacion.service.SincronizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sincronizacion")
@RequiredArgsConstructor
public class SincronizacionController {

    private final SincronizacionService sincronizacionService;

    @PostMapping("/socio")
    public ResponseEntity<SincronizacionResponseDTO> sincronizarSocio(
            @Valid @RequestBody SincronizacionFiltroDTO filtro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sincronizacionService.generarYEnviarPayload(filtro));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<SincronizacionResponseDTO>> listarHistorial() {
        return ResponseEntity.ok(sincronizacionService.listarHistorial());
    }
}
