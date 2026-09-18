package Laboratorio_lex.modules.sincronizacion.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

// F-26: consolidado de actividad por departamento con las 4 métricas exigidas
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoResponseDTO {

    private OffsetDateTime periodoInicio;
    private OffsetDateTime periodoFin;

    private long totalIntentos;
    private long totalAutorizados;
    private long totalDenegados;
    private long totalNoRegistrados;

    private List<ConsolidadoDepartamentoDTO> porDepartamento;
}
