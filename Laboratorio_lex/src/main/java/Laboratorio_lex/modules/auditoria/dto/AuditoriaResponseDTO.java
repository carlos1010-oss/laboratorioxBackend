package Laboratorio_lex.modules.auditoria.dto;

import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaResponseDTO {

    private UUID id;
    private OffsetDateTime timestamp;
    private Long usuarioId;
    private String nombreUsuario;
    private String direccionIp;
    private TipoOperacion tipoOperacion;
    private String moduloTabla;
    private String valorAnterior;
    private String valorNuevo;
}