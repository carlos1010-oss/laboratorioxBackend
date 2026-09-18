package Laboratorio_lex.modules.sincronizacion.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoDepartamentoDTO {

    private Integer departamentoId;
    private String codigo;
    private String nombre;
    private long totalIntentos;
    private long autorizados;
    private long denegados;
    private long noRegistrados;
}
