package Laboratorio_lex.modules.accesos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAccesoRequestDTO {

    @JsonAlias({"documento", "numero_documento"})
    private String numeroDocumento;

    @JsonAlias({"tarjeta", "codigoTarjeta", "codigo_tarjeta_rfid"})
    private String codigoTarjetaRfid;

    @NotNull(message = "El ID del área restringida es obligatorio")
    @JsonAlias({"area_id"})
    private Integer areaId;

    // Getter de conveniencia para aceptar tanto getDocumento como getNumeroDocumento
    public String getDocumento() {
        return numeroDocumento;
    }

    public void setDocumento(String documento) {
        if (this.numeroDocumento == null || this.numeroDocumento.isBlank()) {
            this.numeroDocumento = documento;
        }
    }
}
