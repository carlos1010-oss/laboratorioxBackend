package Laboratorio_lex.modules.accesos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAccesoRequestDTO {

    private String numeroDocumento;

    private String codigoTarjetaRfid;

    @NotNull(message = "El ID del área restringida es obligatorio")
    private Integer areaId;
}
