package Laboratorio_lex.modules.portal.dto;

import lombok.*;

// F-35: verificación pública de registro. Solo confirma si la persona está
// registrada y su estado; no expone datos personales.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificacionIngresoDTO {

    private Boolean registrado;
    private String estado;
    private Boolean autorizadoIngreso;
}