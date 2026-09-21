package Laboratorio_lex.modules.accesos.dto;

import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAccesoResponseDTO {

    private UUID idHistorial;
    private String numeroDocumentoIngresado;
    private String codigoTarjetaRfid;
    private String nombreEmpleado;
    private String estadoEmpleado; // New field
    private String nombreArea;
    private ResultadoAcceso resultado; // AUTORIZADO, DENEGADO o NO_REGISTRADO

    // F-22: semáforo para la respuesta visual (VERDE / ROJO / AMARILLO)
    private String color;
    private String motivo;
    private OffsetDateTime fechaHora;

    // Datos de origen del intento (F-23, F-33)
    private String ipOrigen;
    private String userAgent;
}
