package Laboratorio_lex.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    private String documento;

    private String correo;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;
}
