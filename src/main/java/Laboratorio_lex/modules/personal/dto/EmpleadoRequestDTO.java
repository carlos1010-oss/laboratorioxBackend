package Laboratorio_lex.modules.personal.dto;

import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoRequestDTO {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|PASAPORTE|PPT|TI", message = "El tipo de documento debe ser CC, CE, PASAPORTE, PPT o TI")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no debe superar 20 caracteres")
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @Email(message = "El formato del correo es inválido")
    private String correo;

    private String telefono;

    @NotNull(message = "El ID del departamento es obligatorio")
    private Integer departamentoId;

    @Size(max = 50, message = "El código de tarjeta no debe superar 50 caracteres")
    private String codigoTarjetaRfid;

    // F-11: el estado puede indicarse al registrar; si es null se asume ACTIVO
    private EstadoEmpleado estado;

    // F-17: obligatorio si el estado no es ACTIVO
    private String motivoCambioEstado;
}