package Laboratorio_lex.modules.accesos.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionResponseDTO {

    private Long id;
    private Long empleadoId;
    private String nombreEmpleado;
    private Integer areaId;
    private String nombreArea;
    private String asignadoPorUsuario;
    private Boolean activo;
    private OffsetDateTime fechaAsignacion;
}