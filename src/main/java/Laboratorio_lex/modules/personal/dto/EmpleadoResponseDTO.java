package Laboratorio_lex.modules.personal.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoResponseDTO {

    private Long id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private Integer departamentoId;
    private String departamentoNombre;
    private String codigoTarjetaRfid;
    private String estado;
    private String motivoCambioEstado;
    private OffsetDateTime createdAt;
}