package Laboratorio_lex.modules.personal.repository;

import Laboratorio_lex.modules.personal.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {

    // Busca un departamento por su código único (ej: 'DEP-PROD')
    Optional<Departamento> findByCodigo(String codigo);

    // Verifica si existe un departamento registrado con ese código
    boolean existsByCodigo(String codigo);

    // Lista de departamentos activos
    List<Departamento> findAllByActivoTrue();
}
