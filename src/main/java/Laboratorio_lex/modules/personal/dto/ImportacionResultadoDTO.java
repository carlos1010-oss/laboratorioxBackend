package Laboratorio_lex.modules.personal.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportacionResultadoDTO {

    private int totalProcesados;
    private int exitosos;
    private int fallidos;

    @Builder.Default
    private List<String> errores = new ArrayList<>();
}