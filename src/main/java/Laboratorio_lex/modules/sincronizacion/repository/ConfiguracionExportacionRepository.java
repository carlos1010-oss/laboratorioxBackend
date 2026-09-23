package Laboratorio_lex.modules.sincronizacion.repository;

import Laboratorio_lex.modules.sincronizacion.model.ConfiguracionExportacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionExportacionRepository extends JpaRepository<ConfiguracionExportacion, Integer> {

    // Fila única de configuración (sembrada en V2)
    Optional<ConfiguracionExportacion> findFirstByOrderByIdAsc();
}