package Laboratorio_lex.modules.sincronizacion.dto;

import lombok.*;

import java.time.OffsetDateTime;

// F-30: estado del dashboard de sincronización
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSincronizacionDTO {

    private boolean integracionActiva;
    private String urlWebhook;
    private String correoAlerta;
    private boolean modoSimulacion;
    private Integer reintentosMaximos;
    private OffsetDateTime ultimaEjecucion;
    private OffsetDateTime proximaEjecucion;

    private long pendientes;
    private long exitosos;
    private long fallidos;
}