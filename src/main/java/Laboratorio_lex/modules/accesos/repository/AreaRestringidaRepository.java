package Laboratorio_lex.modules.accesos.repository;

import Laboratorio_lex.modules.accesos.model.AreaRestringida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRestringidaRepository extends JpaRepository<AreaRestringida, Integer> {
    Optional<AreaRestringida> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<AreaRestringida> findAllByActivaTrue();

    List<AreaRestringida> findByNivelRiesgo(String nivelRiesgo);
}
