package Laboratorio_lex.modules.accesos.dto;

import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAccesoResponseDTO {

    private Long idHistorial;
    private String codigoTarjetaRfid;
    private String nombreEmpleado;
    private String nombreArea;
    private ResultadoAcceso resultado; // PERMITIDO o DENEGADO
    private String motivo;
    private OffsetDateTime fechaHora;
}
