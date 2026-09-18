package Laboratorio_lex.modules.accesos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionRequestDTO {

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long empleadoId;

    @NotNull(message = "El ID del área restringida es obligatorio")
    private Integer areaId;

    @NotNull(message = "El ID del usuario que asigna es obligatorio")
    private Long asignadoPorId;
}