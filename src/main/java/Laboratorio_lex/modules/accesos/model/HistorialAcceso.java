package Laboratorio_lex.modules.accesos.model;

import Laboratorio_lex.modules.personal.model.Empleado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_accesos", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero_documento_ingresado", length = 20)
    private String numeroDocumentoIngresado;

    @Column(name = "codigo_tarjeta_ingresado", length = 50)
    private String codigoTarjetaIngresado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = true)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = true)
    private AreaRestringida area;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_acceso", nullable = false)
    private ResultadoAcceso resultado;

    @Column(name = "motivo_denegacion", length = 255)
    private String motivoDenegacion;

    @Column(name = "ip_origen")
    private InetAddress ipOrigen;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = OffsetDateTime.now();
        }
    }
}