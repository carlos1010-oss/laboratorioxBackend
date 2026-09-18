package Laboratorio_lex.modules.auth.repository;

import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su correo electrónico (útil para notificaciones)
    Optional<Usuario> findByCorreo(String correo);

    // Busca un usuario por su número de documento (usado por el Login — F-01)
    Optional<Usuario> findByDocumento(String documento);

    // Verifica si ya existe un usuario registrado con determinado correo
    boolean existsByCorreo(String correo);

    // Verifica si ya existe un usuario registrado con determinado documento
    boolean existsByDocumento(String documento);

    // Cuenta usuarios por rol y estado (protección del último Administrador — F-10)
    long countByRol_NombreAndEstado(String nombreRol, EstadoUsuario estado);

    // Usuarios activos de un rol por nombre (destinatarios de alertas — F-29)
    List<Usuario> findByRol_Nombre(String nombreRol);

    // Actualiza la última actividad de la sesión (F-03)
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.ultimaActividad = :momento WHERE u.id = :id")
    void actualizarUltimaActividad(@Param("id") Long id, @Param("momento") OffsetDateTime momento);

    // Invalida los tokens emitidos incrementando la versión y limpiando la actividad (F-04)
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.tokenVersion = u.tokenVersion + 1, u.ultimaActividad = null WHERE u.id = :id")
    void invalidarTokens(@Param("id") Long id);
}