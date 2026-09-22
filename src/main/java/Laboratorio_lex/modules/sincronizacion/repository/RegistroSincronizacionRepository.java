package Laboratorio_lex.modules.sincronizacion.repository;

import Laboratorio_lex.modules.sincronizacion.model.EstadoSincronizacion;
import Laboratorio_lex.modules.sincronizacion.model.RegistroSincronizacionSocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RegistroSincronizacionRepository extends JpaRepository<RegistroSincronizacionSocio, Long> {

    List<RegistroSincronizacionSocio> findByEstado(EstadoSincronizacion estado);

    long countByEstado(EstadoSincronizacion estado);

    @Query(value = "SELECT r FROM zone_control.registro_sincronizacion_socio r WHERE r.estado = :estado::estado_sincronizacion_enum AND r.fecha_proximo_reintento < :fecha", nativeQuery = true)
    List<RegistroSincronizacionSocio> findByEstadoAndFechaProximoReintentoBefore(
            @Param("estado") EstadoSincronizacion estado, @Param("fecha") OffsetDateTime fecha);
}