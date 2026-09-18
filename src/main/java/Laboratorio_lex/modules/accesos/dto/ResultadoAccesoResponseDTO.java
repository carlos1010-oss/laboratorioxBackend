package Laboratorio_lex.modules.accesos.dto;

import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    private String nombreArea;

    private ResultadoAcceso resultado;

    private String color;

    private String motivo;

    private OffsetDateTime fechaHora;

    private String ipOrigen;

    private String userAgent;

    // Aliases serializables para máxima compatibilidad con el frontend Next.js

    @JsonProperty("id")
    public UUID getId() {
        return idHistorial;
    }

    @JsonProperty("empleadoNombre")
    public String getEmpleadoNombre() {
        return nombreEmpleado;
    }

    @JsonProperty("empleadoNombreCompleto")
    public String getEmpleadoNombreCompleto() {
        return nombreEmpleado;
    }

    @JsonProperty("areaNombre")
    public String getAreaNombre() {
        return nombreArea;
    }

    @JsonProperty("resultadoAcceso")
    public ResultadoAcceso getResultadoAcceso() {
        return resultado;
    }

    @JsonProperty("estado")
    public String getEstado() {
        return resultado != null ? resultado.name() : null;
    }

    @JsonProperty("mensaje")
    public String getMensaje() {
        return motivo != null ? motivo : ("AUTORIZADO".equals(String.valueOf(resultado)) ? "Acceso autorizado" : "");
    }

    @JsonProperty("motivoDenegacion")
    public String getMotivoDenegacion() {
        return motivo;
    }

    @JsonProperty("timestamp")
    public OffsetDateTime getTimestamp() {
        return fechaHora;
    }

    @JsonProperty("codigoTarjetaIngresado")
    public String getCodigoTarjetaIngresado() {
        return codigoTarjetaRfid;
    }
}
