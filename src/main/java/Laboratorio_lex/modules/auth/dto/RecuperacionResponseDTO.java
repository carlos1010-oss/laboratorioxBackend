package Laboratorio_lex.modules.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecuperacionResponseDTO {
    private String mensaje;
    private String tokenDemo;
}
