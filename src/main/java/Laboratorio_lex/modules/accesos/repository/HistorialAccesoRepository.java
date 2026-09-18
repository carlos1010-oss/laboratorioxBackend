package Laboratorio_lex.modules.accesos.repository;

import Laboratorio_lex.modules.accesos.model.HistorialAcceso;
import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialAccesoRepository extends JpaRepository<HistorialAcceso, UUID>,
        JpaSpecificationExecutor<HistorialAcceso> {

    // Búsqueda personalizada por documento ingresado
    List<HistorialAcceso> findByNumeroDocumentoIngresado(String numeroDocumentoIngresado);

    // Búsqueda personalizada por código de tarjeta ingresado
    List<HistorialAcceso> findByCodigoTarjetaIngresado(String codigoTarjetaIngresado);

    // Búsqueda personalizada por empleado
    List<HistorialAcceso> findByEmpleadoId(Long empleadoId);

    // Búsqueda personalizada por área
    List<HistorialAcceso> findByAreaId(Integer areaId);

    // Consulta de accesos por rango de fechas (útil para auditoría e integración internacional)
    List<HistorialAcceso> findByTimestampBetween(OffsetDateTime inicio, OffsetDateTime fin);

    // Total de intentos en un periodo (F-26)
    long countByTimestampGreaterThanEqualAndTimestampLessThan(OffsetDateTime inicio, OffsetDateTime fin);

    // Total de intentos por resultado en un periodo (F-26)
    long countByResultadoAndTimestampGreaterThanEqualAndTimestampLessThan(ResultadoAcceso resultado,
            OffsetDateTime inicio, OffsetDateTime fin);

    // Consolidado por departamento con las 4 métricas (F-26)
    @Query("SELECT d.id AS departamentoId, d.codigo AS codigo, d.nombre AS nombre, "
            + "COUNT(h) AS totalIntentos, "
            + "SUM(CASE WHEN h.resultado = :autorizado THEN 1 ELSE 0 END) AS autorizados, "
            + "SUM(CASE WHEN h.resultado = :denegado THEN 1 ELSE 0 END) AS denegados, "
            + "SUM(CASE WHEN h.resultado = :noRegistrado THEN 1 ELSE 0 END) AS noRegistrados "
            + "FROM HistorialAcceso h JOIN h.empleado e JOIN e.departamento d "
            + "WHERE h.timestamp >= :inicio AND h.timestamp < :fin "
            + "GROUP BY d.id, d.codigo, d.nombre ORDER BY d.nombre")
    List<ConsolidadoDepartamentoProjection> consolidarPorDepartamento(@Param("inicio") OffsetDateTime inicio,
            @Param("fin") OffsetDateTime fin, @Param("autorizado") ResultadoAcceso autorizado,
            @Param("denegado") ResultadoAcceso denegado, @Param("noRegistrado") ResultadoAcceso noRegistrado);
}
