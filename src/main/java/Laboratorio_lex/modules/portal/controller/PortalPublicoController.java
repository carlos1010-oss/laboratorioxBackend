package Laboratorio_lex.modules.portal.controller;

import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoPublicoDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.service.AccesoService;
import Laboratorio_lex.modules.portal.dto.VerificacionIngresoDTO;
import Laboratorio_lex.modules.portal.service.PortalPublicoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// F-35: portal público (sin autenticación). Permite simular el acceso del
// molinete y verificar si una persona está registrada, sin exponer datos
// personales. Pensado para kioscos o la página pública del frontend.
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
public class PortalPublicoController {

    private final AccesoService accesoService;
    private final PortalPublicoService portalPublicoService;

    @PostMapping("/accesos/molinete")
    public ResponseEntity<ResultadoAccesoPublicoDTO> simularAcceso(
            @Valid @RequestBody RegistroAccesoRequestDTO dto,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpOrigen(request);
        String userAgent = request.getHeader("User-Agent");

        ResultadoAccesoResponseDTO interno = accesoService.procesarAccesoMolinete(dto, ipOrigen, userAgent);
        return ResponseEntity.ok(ResultadoAccesoPublicoDTO.desdeInterno(interno));
    }

    @GetMapping("/verificacion")
    public ResponseEntity<VerificacionIngresoDTO> verificar(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String tarjeta) {
        return ResponseEntity.ok(portalPublicoService.verificarRegistro(documento, tarjeta));
    }

    private String obtenerIpOrigen(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}