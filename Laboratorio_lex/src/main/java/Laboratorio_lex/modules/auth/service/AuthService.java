package Laboratorio_lex.modules.auth.service;

import Laboratorio_lex.config.JwtProvider;
import Laboratorio_lex.modules.auth.dto.LoginRequestDTO;
import Laboratorio_lex.modules.auth.dto.LoginResponseDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioResponseDTO;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. Buscar usuario por correo
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // 2. Verificar si la cuenta está bloqueada o inactiva (F-08)
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new RuntimeException("La cuenta se encuentra BLOQUEADA por superar el límite de intentos fallidos");
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new RuntimeException("La cuenta se encuentra INACTIVA. Contacte al administrador");
        }

        // 3. Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            int intentosActuales = usuario.getIntentosFallidos() != null ? usuario.getIntentosFallidos() : 0;
            int nuevosIntentos = intentosActuales + 1;
            usuario.setIntentosFallidos((short) nuevosIntentos);

            if (nuevosIntentos >= 3) {
                usuario.setEstado(EstadoUsuario.BLOQUEADO);
                usuarioRepository.save(usuario);
                throw new RuntimeException("Cuenta BLOQUEADA tras 3 intentos fallidos consecutivos");
            }

            usuarioRepository.save(usuario);
            throw new RuntimeException("Credenciales inválidas. Intentos fallidos: " + nuevosIntentos + "/3");
        }

        // 4. Reiniciar contador de intentos fallidos
        if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0) {
            usuario.setIntentosFallidos((short) 0);
            usuarioRepository.save(usuario);
        }

        // 5. Generar token JWT
        String tokenReal = jwtProvider.generateToken(usuario.getCorreo());

        // 6. Mapear respuesta a DTO
        UsuarioResponseDTO usuarioDTO = UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .documento(usuario.getDocumento())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .estado(usuario.getEstado().name())
                .rol(usuario.getRol().getNombre())
                .build();

        return LoginResponseDTO.builder()
                .token(tokenReal)
                .tipoToken("Bearer")
                .usuario(usuarioDTO)
                .build();
    }
}