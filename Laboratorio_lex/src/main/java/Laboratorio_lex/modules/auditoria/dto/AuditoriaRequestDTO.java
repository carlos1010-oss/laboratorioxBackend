package Laboratorio_lex.modules.auditoria.dto;

import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaRequestDTO {

    private Long usuarioId;
    private String direccionIp;
    private TipoOperacion tipoOperacion;
    private String moduloTabla;
    private String valorAnterior;
    private String valorNuevo;
}