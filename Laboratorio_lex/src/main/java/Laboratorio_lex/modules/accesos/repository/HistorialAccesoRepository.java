package Laboratorio_lex.modules.accesos.repository;

import Laboratorio_lex.modules.accesos.model.HistorialAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface HistorialAccesoRepository extends JpaRepository<HistorialAcceso, Long> {

    // Consulta de accesos por rango de fechas (útil para auditoría e integración
    // internacional)
    List<HistorialAcceso> findByFechaHoraBetween(OffsetDateTime inicio, OffsetDateTime fin);
}