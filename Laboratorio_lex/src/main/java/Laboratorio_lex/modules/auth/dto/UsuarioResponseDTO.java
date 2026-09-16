package Laboratorio_lex.modules.auth.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String documento;
    private String nombres;
    private String apellidos;
    private String correo;
    private String estado;
    private String rol;
    private OffsetDateTime createdAt;
}