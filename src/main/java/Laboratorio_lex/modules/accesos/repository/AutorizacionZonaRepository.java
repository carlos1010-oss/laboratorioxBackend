package Laboratorio_lex.modules.accesos.repository;

import Laboratorio_lex.modules.accesos.model.AutorizacionZona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutorizacionZonaRepository extends JpaRepository<AutorizacionZona, Long> {

    boolean existsByEmpleadoIdAndAreaIdAndActivoTrue(Long empleadoId, Integer areaId);

    Optional<AutorizacionZona> findByEmpleadoIdAndAreaId(Long empleadoId, Integer areaId);

    List<AutorizacionZona> findByEmpleadoId(Long empleadoId);
}