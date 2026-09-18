package Laboratorio_lex.modules.catalogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartamentoRequestDTO {

    @NotBlank(message = "El código del departamento es obligatorio")
    @Size(max = 20, message = "El código no debe superar 20 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre del departamento es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
    private String descripcion;
}
