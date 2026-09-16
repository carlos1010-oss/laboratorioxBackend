package Laboratorio_lex.modules.sincronizacion.dto;

import Laboratorio_lex.modules.sincronizacion.model.EstadoSincronizacion;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SincronizacionResponseDTO {

    private Long id;
    private OffsetDateTime periodoInicio;
    private OffsetDateTime periodoFin;
    private Integer departamentoId;
    private String nombreDepartamento;
    private String payloadJson;
    private EstadoSincronizacion estado;
    private Short intentosRealizados;
    private Short codigoRespuestaHttp;
    private OffsetDateTime fechaEnvio;
    private OffsetDateTime fechaProximoReintento;
    private OffsetDateTime createdAt;
}