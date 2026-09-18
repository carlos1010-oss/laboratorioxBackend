package Laboratorio_lex.modules.catalogos.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaRestringidaResponseDTO {

    private Integer id;
    private String codigo;
    private String nombre;
    private String nivelRiesgo;
    private String descripcion;
    private Boolean activa;
    private OffsetDateTime createdAt;
}
