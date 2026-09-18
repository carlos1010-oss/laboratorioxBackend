package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.modules.accesos.model.HistorialAcceso;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// F-24: construcción de filtros dinámicos para el historial de accesos
public final class HistorialSpecifications {

    private HistorialSpecifications() {
    }

    public static Specification<HistorialAcceso> conFiltros(
            String numeroDocumento, Integer areaId, OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (numeroDocumento != null && !numeroDocumento.isBlank()) {
                predicados.add(cb.equal(root.get("numeroDocumentoIngresado"), numeroDocumento));
            }
            if (areaId != null) {
                predicados.add(cb.equal(root.get("area").get("id"), areaId));
            }
            if (fechaInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fechaInicio));
            }
            if (fechaFin != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("timestamp"), fechaFin));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
