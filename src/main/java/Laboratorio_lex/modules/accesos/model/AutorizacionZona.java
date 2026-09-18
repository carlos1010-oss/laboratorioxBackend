package Laboratorio_lex.modules.accesos.model;

import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.personal.model.Empleado;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "autorizaciones_zona", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaRestringida area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por", nullable = false)
    private Usuario asignadoPor;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private OffsetDateTime fechaAsignacion;

    @Column(nullable = false)
    private Boolean activo;

    @PrePersist
    protected void onCreate() {
        this.fechaAsignacion = OffsetDateTime.now();
        if (this.activo == null)
            this.activo = true;
    }
}