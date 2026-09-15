package Laboratorio_lex.modules.personal.repository;

import Laboratorio_lex.modules.personal.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    // Busca un empleado por su número de documento (Inmutable - F-16)
    Optional<Empleado> findByNumeroDocumento(String numeroDocumento);

    // Busca un empleado por el código de su tarjeta RFID (Simulador de accesos -
    // F-19)
    Optional<Empleado> findByCodigoTarjetaRfid(String codigoTarjetaRfid);

    // Validaciones de existencia antes de registrar o procesar CSV
    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByCodigoTarjetaRfid(String codigoTarjetaRfid);

    // Búsqueda para filtros en pantalla por departamento (F-34)
    List<Empleado> findByDepartamentoId(Integer departamentoId);
}