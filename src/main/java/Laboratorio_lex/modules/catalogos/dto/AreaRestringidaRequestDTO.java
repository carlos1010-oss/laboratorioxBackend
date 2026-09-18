package Laboratorio_lex.modules.catalogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaRestringidaRequestDTO {

    @NotBlank(message = "El código del área es obligatorio")
    @Size(max = 20, message = "El código no debe superar 20 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre del área es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "El nivel de riesgo es obligatorio")
    @Pattern(regexp = "BAJO|MEDIO|ALTO|CRITICO", message = "El nivel de riesgo debe ser BAJO, MEDIO, ALTO o CRITICO")
    private String nivelRiesgo;

    @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
    private String descripcion;
}
