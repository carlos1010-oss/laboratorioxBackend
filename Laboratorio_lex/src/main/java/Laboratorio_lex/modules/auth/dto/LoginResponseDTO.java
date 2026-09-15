package Laboratorio_lex.modules.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private String tipoToken; // Ejemplo: "Bearer"
    private UsuarioResponseDTO usuario;
}