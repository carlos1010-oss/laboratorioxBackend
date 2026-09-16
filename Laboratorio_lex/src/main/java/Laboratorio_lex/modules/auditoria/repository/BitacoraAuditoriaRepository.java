package Laboratorio_lex.modules.auditoria.repository;

import Laboratorio_lex.modules.auditoria.model.BitacoraAuditoria;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, UUID> {

    List<BitacoraAuditoria> findByUsuarioId(Long usuarioId);

    List<BitacoraAuditoria> findByTipoOperacion(TipoOperacion tipoOperacion);

    List<BitacoraAuditoria> findByTimestampBetween(OffsetDateTime inicio, OffsetDateTime fin);
}