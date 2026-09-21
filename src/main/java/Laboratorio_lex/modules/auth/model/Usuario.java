package Laboratorio_lex.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios", schema = "zone_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    
    @Column(nullable = false)
    private EstadoUsuario estado;

    @Column(name = "intentos_fallidos", nullable = false)
    private Short intentosFallidos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Última actividad registrada; la sesión expira por inactividad (F-03)
    @Column(name = "ultima_actividad")
    private OffsetDateTime ultimaActividad;

    // Versión del token vigente; se incrementa al cerrar sesión (F-04)
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.intentosFallidos == null) {
            this.intentosFallidos = (short) 0;
        }
        if (this.tokenVersion == null) {
            this.tokenVersion = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}