package Laboratorio_lex.modules.sincronizacion.model;

import Laboratorio_lex.modules.personal.model.Departamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registro_sincronizacion_socio", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroSincronizacionSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo_inicio", nullable = false)
    private OffsetDateTime periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private OffsetDateTime periodoFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    
    @Column(nullable = false)
    private EstadoSincronizacion estado;

    @Column(name = "intentos_realizados", nullable = false)
    private Short intentosRealizados;

    @Column(name = "codigo_respuesta_http")
    private Short codigoRespuestaHttp;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "fecha_proximo_reintento")
    private OffsetDateTime fechaProximoReintento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.intentosRealizados == null) {
            this.intentosRealizados = (short) 0;
        }
        if (this.estado == null) {
            this.estado = EstadoSincronizacion.EN_REINTENTO;
        }
    }
}