package Laboratorio_lex.modules.accesos.repository;

import Laboratorio_lex.modules.accesos.model.AutorizacionZona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutorizacionZonaRepository extends JpaRepository<AutorizacionZona, Long> {

    // Verifica si un empleado tiene permiso ACTIVO para ingresar a un área
    // específica
    Optional<AutorizacionZona> findByEmpleadoIdAndAreaIdAndActivoTrue(Long empleadoId, Integer areaId);

    boolean existsByEmpleadoIdAndAreaIdAndActivoTrue(Long empleadoId, Integer areaId);
}