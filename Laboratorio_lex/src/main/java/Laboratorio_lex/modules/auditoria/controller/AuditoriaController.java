package Laboratorio_lex.modules.auditoria.controller;

import Laboratorio_lex.modules.auditoria.dto.AuditoriaResponseDTO;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<List<AuditoriaResponseDTO>> listarEventos() {
        return ResponseEntity.ok(auditoriaService.obtenerTodos());
    }
}