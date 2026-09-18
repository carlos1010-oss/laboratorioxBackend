package Laboratorio_lex.modules.accesos.dto;

import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import lombok.*;

import java.time.OffsetDateTime;

// F-35: respuesta pública de la simulación de acceso. No expone datos personales
// (nombre, correo, etc.): solo el resultado, el semáforo y el motivo.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAccesoPublicoDTO {

    private String numeroDocumentoIngresado;
    private String codigoTarjetaRfid;
    private String nombreArea;
    private ResultadoAcceso resultado;
    private String color;
    private String motivo;
    private OffsetDateTime fechaHora;

    public static ResultadoAccesoPublicoDTO desdeInterno(ResultadoAccesoResponseDTO interno) {
        return ResultadoAccesoPublicoDTO.builder()
                .numeroDocumentoIngresado(interno.getNumeroDocumentoIngresado())
                .codigoTarjetaRfid(interno.getCodigoTarjetaRfid())
                .nombreArea(interno.getNombreArea())
                .resultado(interno.getResultado())
                .color(interno.getColor())
                .motivo(interno.getMotivo())
                .fechaHora(interno.getFechaHora())
                .build();
    }
}