package Laboratorio_lex.modules.auditoria.repository;

import Laboratorio_lex.modules.auditoria.model.BitacoraAuditoria;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// F-33: construcción de consultas con filtros opcionales (usuario, tipo de
// operación, módulo/tabla y rango de fechas).
public final class AuditoriaSpecifications {

    private AuditoriaSpecifications() {
    }

    public static Specification<BitacoraAuditoria> conFiltros(
            Long usuarioId,
            TipoOperacion tipoOperacion,
            String moduloTabla,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }
            if (tipoOperacion != null) {
                predicates.add(cb.equal(root.get("tipoOperacion"), tipoOperacion));
            }
            if (moduloTabla != null && !moduloTabla.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("moduloTabla")),
                        "%" + moduloTabla.toLowerCase() + "%"));
            }
            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fechaInicio));
            }
            if (fechaFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), fechaFin));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}