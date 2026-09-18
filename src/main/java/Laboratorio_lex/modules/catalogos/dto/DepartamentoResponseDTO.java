package Laboratorio_lex.modules.catalogos.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartamentoResponseDTO {

    private Integer id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private OffsetDateTime createdAt;
}
