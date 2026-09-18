package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auditoria.util.AuditoriaContexto;
import Laboratorio_lex.modules.auth.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// F-25: exportación del historial de accesos
@Service
@RequiredArgsConstructor
public class HistorialExportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] CABECERAS = {
            "id_historial", "fecha_hora", "documento_ingresado", "tarjeta_ingresada",
            "empleado", "area", "resultado", "motivo", "ip_origen", "user_agent"
    };

    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;
    private final AuditoriaContexto auditoriaContexto;

    public byte[] exportar(String formato, String numeroDocumento, Integer areaId,
            OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        if (formato == null || formato.isBlank()) {
            formato = "csv";
        }
        List<ResultadoAccesoResponseDTO> registros = accesoService.listarParaExportar(numeroDocumento, areaId,
                fechaInicio, fechaFin);

        // Registro en la bitácora de auditoría (GxP / 21 CFR Part 11: trazabilidad de descargas)
        registrarAuditoriaDescarga(formato, registros.size());

        return switch (formato.toLowerCase()) {
            case "csv" -> exportarCsv(registros);
            case "html" -> exportarHtml(registros).getBytes(StandardCharsets.UTF_8);
            case "pdf" -> throw new BadRequestException(
                    "La exportación a PDF está diferida por decisión de diseño; use 'csv' o 'html' (imprimible a PDF desde el navegador).");
            default -> throw new BadRequestException("Formato de exportación no soportado: " + formato);
        };
    }

    private void registrarAuditoriaDescarga(String formato, int totalRegistros) {
        try {
            Usuario usuarioActual = auditoriaContexto.obtenerUsuarioActual();
            AuditoriaRequestDTO dto = AuditoriaRequestDTO.builder()
                    .usuarioId(usuarioActual != null ? usuarioActual.getId() : null)
                    .direccionIp(auditoriaContexto.obtenerIpActual())
                    .tipoOperacion(TipoOperacion.DESCARGA)
                    .moduloTabla("historial_accesos")
                    .valorAnterior(null)
                    .valorNuevo("{\"formato\":\"" + formato + "\",\"registrosExportados\":" + totalRegistros + "}")
                    .build();

            auditoriaService.registrarEvento(dto);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de descarga: " + e.getMessage());
        }
    }

    private byte[] exportarCsv(List<ResultadoAccesoResponseDTO> registros) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // BOM UTF-8 para que Excel reconozca acentos
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                            .setHeader(CABECERAS)
                            .build())) {

                for (ResultadoAccesoResponseDTO r : registros) {
                    printer.printRecord(
                            r.getIdHistorial(),
                            formatear(r.getFechaHora()),
                            r.getNumeroDocumentoIngresado(),
                            r.getCodigoTarjetaRfid(),
                            r.getNombreEmpleado(),
                            r.getNombreArea(),
                            r.getResultado(),
                            r.getMotivo(),
                            r.getIpOrigen(),
                            r.getUserAgent());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el CSV del historial: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // Alternativa sin dependencias externas: documento HTML listo para imprimir a PDF con el navegador
    private String exportarHtml(List<ResultadoAccesoResponseDTO> registros) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<title>Historial de accesos</title>")
                .append("<style>body{font-family:Arial,sans-serif;font-size:12px;margin:24px;}")
                .append("h1{font-size:18px;}table{border-collapse:collapse;width:100%;}")
                .append("th,td{border:1px solid #999;padding:4px 6px;text-align:left;}")
                .append("th{background:#eee;}tr.VERDE td.resultado{color:#0a7d28;font-weight:bold;}")
                .append("tr.ROJO td.resultado{color:#c1121f;font-weight:bold;}")
                .append("tr.AMARILLO td.resultado{color:#b58900;font-weight:bold;}")
                .append("@media print{button{display:none;}}</style></head><body>")
                .append("<h1>Historial de accesos - Zone Control</h1>")
                .append("<p>Generado: ").append(formatear(OffsetDateTime.now())).append("</p>")
                .append("<table><thead><tr><th>Fecha/hora</th><th>Documento</th><th>Tarjeta</th>")
                .append("<th>Empleado</th><th>Área</th><th>Resultado</th><th>Motivo</th><th>IP</th></tr></thead><tbody>");

        for (ResultadoAccesoResponseDTO r : registros) {
            sb.append("<tr class=\"").append(escapar(r.getColor())).append("\">")
                    .append("<td>").append(escapar(formatear(r.getFechaHora()))).append("</td>")
                    .append("<td>").append(escapar(r.getNumeroDocumentoIngresado())).append("</td>")
                    .append("<td>").append(escapar(r.getCodigoTarjetaRfid())).append("</td>")
                    .append("<td>").append(escapar(r.getNombreEmpleado())).append("</td>")
                    .append("<td>").append(escapar(r.getNombreArea())).append("</td>")
                    .append("<td class=\"resultado\">").append(escapar(String.valueOf(r.getResultado()))).append("</td>")
                    .append("<td>").append(escapar(r.getMotivo())).append("</td>")
                    .append("<td>").append(escapar(r.getIpOrigen())).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table><p><button onclick=\"window.print()\">Imprimir / Guardar como PDF</button></p>")
                .append("</body></html>");
        return sb.toString();
    }

    private String formatear(OffsetDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
