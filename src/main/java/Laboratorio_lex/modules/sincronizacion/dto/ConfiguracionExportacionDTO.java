package Laboratorio_lex.modules.sincronizacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionExportacionDTO {

    private Short id;

    @NotBlank(message = "La frecuencia es obligatoria")
    private String frecuencia;

    private Short diaSemana;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotBlank(message = "El formato es obligatorio")
    private String formato;

    private String urlDestino;

    private String correoAlerta;

    private Boolean activo;

    private OffsetDateTime fechaProximaEjecucion;

    private OffsetDateTime ultimaEjecucion;
}