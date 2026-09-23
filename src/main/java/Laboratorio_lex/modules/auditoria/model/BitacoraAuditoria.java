package Laboratorio_lex.modules.auditoria.model;

import Laboratorio_lex.modules.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bitacora_auditoria", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", nullable = false)
    private TipoOperacion tipoOperacion;

    @Column(name = "modulo_tabla", nullable = false, length = 100)
    private String moduloTabla;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior", columnDefinition = "jsonb")
    private String valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo", columnDefinition = "jsonb")
    private String valorNuevo;

    @PrePersist
    protected void onCreate() {
        this.timestamp = OffsetDateTime.now();
    }
}