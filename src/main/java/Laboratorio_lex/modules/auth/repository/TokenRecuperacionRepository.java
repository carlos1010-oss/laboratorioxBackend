package Laboratorio_lex.modules.auth.repository;

import Laboratorio_lex.modules.auth.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    Optional<TokenRecuperacion> findByTokenHashAndUsadoFalse(String tokenHash);

    Optional<TokenRecuperacion> findByTokenHashAndUsadoFalseAndExpiracionAfter(String tokenHash, OffsetDateTime ahora);

    List<TokenRecuperacion> findByUsuarioId(Long usuarioId);
}
