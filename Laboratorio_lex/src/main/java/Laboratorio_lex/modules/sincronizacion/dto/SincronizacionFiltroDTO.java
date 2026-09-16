package Laboratorio_lex.modules.sincronizacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SincronizacionFiltroDTO {

    @NotNull(message = "La fecha de inicio del periodo es obligatoria")
    private OffsetDateTime periodoInicio;

    @NotNull(message = "La fecha de fin del periodo es obligatoria")
    private OffsetDateTime periodoFin;

    private Integer departamentoId;
}