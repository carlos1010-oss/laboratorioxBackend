package Laboratorio_lex.modules.accesos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAccesoRequestDTO {

    @NotBlank(message = "El código de la tarjeta RFID es obligatorio")
    private String codigoTarjetaRfid;

    @NotNull(message = "El ID del área restringida es obligatorio")
    private Integer areaId;
}
