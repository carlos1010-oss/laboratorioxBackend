package Laboratorio_lex.modules.auth.repository;

import Laboratorio_lex.modules.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su correo electrónico (útil para el Login — F-01)
    Optional<Usuario> findByCorreo(String correo);

    // Busca un usuario por su número de documento (útil para validaciones de
    // identidad — F-02)
    Optional<Usuario> findByDocumento(String documento);

    // Verifica si ya existe un usuario registrado con determinado correo
    boolean existsByCorreo(String correo);

    // Verifica si ya existe un usuario registrado con determinado documento
    boolean existsByDocumento(String documento);
}