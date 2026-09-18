package Laboratorio_lex.modules.auditoria.controller;

import Laboratorio_lex.modules.auditoria.dto.AuditoriaResponseDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    // F-33: listado paginado y filtrable por usuario, tipo de operación,
    // módulo/tabla y rango de fechas (yyyy-MM-dd, día completo).
    @GetMapping({"", "/bitacora"})
    public ResponseEntity<Page<AuditoriaResponseDTO>> listarEventos(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) TipoOperacion tipoOperacion,
            @RequestParam(required = false) String moduloTabla,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditoriaService.obtenerPaginado(
                usuarioId, tipoOperacion, moduloTabla, fechaInicio, fechaFin, page, size));
    }
}