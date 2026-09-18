package Laboratorio_lex.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "El número de documento es obligatorio")
    private String documento;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;
}