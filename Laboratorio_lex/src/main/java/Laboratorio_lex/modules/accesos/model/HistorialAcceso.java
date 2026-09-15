package Laboratorio_lex.modules.accesos.model;

import Laboratorio_lex.modules.personal.model.Empleado;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_accesos", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado; // Puede ser null si la tarjeta o documento no está registrado

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaRestringida area;

    @Column(name = "codigo_tarjeta_presentada", length = 50)
    private String codigoTarjetaPresentada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoAcceso resultado;

    @Column(name = "motivo_denegacion", length = 255)
    private String motivoDenegacion;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime fechaHora;

    @PrePersist
    protected void onCreate() {
        this.fechaHora = OffsetDateTime.now();
    }
}