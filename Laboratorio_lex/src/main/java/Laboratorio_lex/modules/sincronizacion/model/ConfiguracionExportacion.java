package Laboratorio_lex.modules.sincronizacion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "configuracion_exportacion", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionExportacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    // DIARIA | SEMANAL | MENSUAL (CHECK en BD)
    @Column(nullable = false)
    private String frecuencia;

    @Column(name = "dia_semana")
    private Short diaSemana;

    @Column(nullable = false)
    private LocalTime hora;

    // Solo JSON por ahora (CHECK en BD)
    @Column(nullable = false)
    private String formato;

    @Column(name = "url_destino")
    private String urlDestino;

    @Column(name = "correo_alerta")
    private String correoAlerta;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_proxima_ejecucion")
    private OffsetDateTime fechaProximaEjecucion;

    @Column(name = "ultima_ejecucion")
    private OffsetDateTime ultimaEjecucion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.activo == null) {
            this.activo = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}