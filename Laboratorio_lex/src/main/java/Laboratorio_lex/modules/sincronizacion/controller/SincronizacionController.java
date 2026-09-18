package Laboratorio_lex.modules.sincronizacion.controller;

import Laboratorio_lex.modules.sincronizacion.dto.ConsolidadoResponseDTO;
import Laboratorio_lex.modules.sincronizacion.dto.ConfiguracionExportacionDTO;
import Laboratorio_lex.modules.sincronizacion.dto.DashboardSincronizacionDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionFiltroDTO;
import Laboratorio_lex.modules.sincronizacion.dto.SincronizacionResponseDTO;
import Laboratorio_lex.modules.sincronizacion.service.ConsolidadoService;
import Laboratorio_lex.modules.sincronizacion.service.SincronizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sincronizacion")
@RequiredArgsConstructor
public class SincronizacionController {

    private final SincronizacionService sincronizacionService;
    private final ConsolidadoService consolidadoService;

    // F-27/F-28: genera el consolidado y lo envía al socio (simulado por defecto)
    @PostMapping("/socio")
    public ResponseEntity<SincronizacionResponseDTO> sincronizarSocio(
            @Valid @RequestBody SincronizacionFiltroDTO filtro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sincronizacionService.generarYEnviarPayload(filtro));
    }

    // F-26: consolidado de actividad por departamento (alimenta el dashboard y el payload)
    @GetMapping("/consolidado")
    public ResponseEntity<ConsolidadoResponseDTO> consolidado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin) {
        return ResponseEntity.ok(consolidadoService.generarConsolidado(inicio, fin));
    }

    // F-30: estado general de la integración para el dashboard
    @GetMapping("/estado")
    public ResponseEntity<DashboardSincronizacionDTO> estado() {
        return ResponseEntity.ok(sincronizacionService.obtenerEstado());
    }

    // F-30: configuración de la exportación periódica (fila única sembrada en V2)
    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionExportacionDTO> configuracion() {
        return ResponseEntity.ok(sincronizacionService.obtenerConfiguracion());
    }

    @PutMapping("/configuracion")
    public ResponseEntity<ConfiguracionExportacionDTO> actualizarConfiguracion(
            @Valid @RequestBody ConfiguracionExportacionDTO dto) {
        return ResponseEntity.ok(sincronizacionService.actualizarConfiguracion(dto));
    }

    // F-30: reenvío manual de un registro EN_REINTENTO o FALLIDO
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<SincronizacionResponseDTO> reenviar(@PathVariable Long id) {
        return ResponseEntity.ok(sincronizacionService.reenviarManualmente(id));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<SincronizacionResponseDTO>> listarHistorial() {
        return ResponseEntity.ok(sincronizacionService.listarHistorial());
    }
}