package Laboratorio_lex.modules.accesos.controller;

import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.service.AccesoService;
import Laboratorio_lex.modules.accesos.service.HistorialExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/accesos")
@RequiredArgsConstructor
public class AccesoController {

    private final AccesoService accesoService;
    private final HistorialExportService historialExportService;

    // F-20 / F-21: simulación de acceso físico en molinete y verificación directa desde simulador
    @PostMapping({"/molinete", "/verificar"})
    public ResponseEntity<ResultadoAccesoResponseDTO> registrarAccesoMolinete(
            @Valid @RequestBody RegistroAccesoRequestDTO dto,
            HttpServletRequest request) {
        String ipOrigen = obtenerIpOrigen(request);
        String userAgent = request.getHeader("User-Agent");
        // Canal interno: doble factor para empleados + maestro para Admin/Supervisor
        return ResponseEntity.ok(accesoService.procesarAccesoMolinete(dto, ipOrigen, userAgent, true, true));
    }

    // F-24: historial filtrado por documento, área y rango de fechas
    // Soporta retorno directo de array (si no se envía page/size) o paginación estándar
    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorial(
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaFin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        if (page == null && size == null) {
            return ResponseEntity.ok(
                    accesoService.listarParaExportar(numeroDocumento, areaId, fechaInicio, fechaFin));
        }

        return ResponseEntity.ok(
                accesoService.buscarHistorial(numeroDocumento, areaId, fechaInicio, fechaFin, pageable));
    }

    // F-25: exportación del historial filtrado (csv | html imprimible)
    @GetMapping("/historial/exportar")
    public ResponseEntity<byte[]> exportarHistorial(
            @RequestParam(defaultValue = "csv") String formato,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaFin) {

        byte[] contenido = historialExportService.exportar(formato, numeroDocumento, areaId, fechaInicio, fechaFin);

        boolean esHtml = "html".equalsIgnoreCase(formato);
        MediaType mediaType = esHtml
                ? new MediaType("text", "html", StandardCharsets.UTF_8)
                : new MediaType("text", "csv", StandardCharsets.UTF_8);
        String extension = esHtml ? "html" : "csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"historial_accesos." + extension + "\"")
                .contentType(mediaType)
                .body(contenido);
    }

    private String obtenerIpOrigen(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
