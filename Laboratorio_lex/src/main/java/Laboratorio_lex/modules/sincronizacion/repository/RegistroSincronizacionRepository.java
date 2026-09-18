package Laboratorio_lex.modules.sincronizacion.repository;

import Laboratorio_lex.modules.sincronizacion.model.EstadoSincronizacion;
import Laboratorio_lex.modules.sincronizacion.model.RegistroSincronizacionSocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RegistroSincronizacionRepository extends JpaRepository<RegistroSincronizacionSocio, Long> {

    List<RegistroSincronizacionSocio> findByEstado(EstadoSincronizacion estado);

    long countByEstado(EstadoSincronizacion estado);

    List<RegistroSincronizacionSocio> findByEstadoAndFechaProximoReintentoBefore(
            EstadoSincronizacion estado, OffsetDateTime fecha);
}