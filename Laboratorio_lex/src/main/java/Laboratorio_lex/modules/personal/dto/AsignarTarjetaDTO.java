package Laboratorio_lex.modules.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignarTarjetaDTO {

    // F-19: el número de la tarjeta RFID/NFC simula la credencial física
    @NotBlank(message = "El código de la tarjeta RFID/NFC es obligatorio")
    @Size(max = 50, message = "El código de tarjeta no debe superar 50 caracteres")
    private String codigoTarjetaRfid;
}
