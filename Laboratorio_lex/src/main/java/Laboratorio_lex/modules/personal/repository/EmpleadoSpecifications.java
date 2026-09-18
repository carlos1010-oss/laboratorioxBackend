package Laboratorio_lex.modules.personal.repository;

import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// F-34: construcción de consultas con filtros opcionales (documento, nombres,
// apellidos, departamento y estado) para la búsqueda paginada de personal.
public final class EmpleadoSpecifications {

    private EmpleadoSpecifications() {
    }

    public static Specification<Empleado> conFiltros(
            String documento,
            String nombres,
            String apellidos,
            Integer departamentoId,
            EstadoEmpleado estado) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (documento != null && !documento.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("numeroDocumento")),
                        "%" + documento.toLowerCase() + "%"));
            }
            if (nombres != null && !nombres.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombres")),
                        "%" + nombres.toLowerCase() + "%"));
            }
            if (apellidos != null && !apellidos.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidos")),
                        "%" + apellidos.toLowerCase() + "%"));
            }
            if (departamentoId != null) {
                predicates.add(cb.equal(root.get("departamento").get("id"), departamentoId));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}