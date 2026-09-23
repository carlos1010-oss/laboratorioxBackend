package Laboratorio_lex.modules.auth.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolResponseDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private OffsetDateTime createdAt;
}
