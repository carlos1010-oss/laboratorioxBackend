package Laboratorio_lex.modules.auth.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.CredencialesInvalidasException;
import Laboratorio_lex.common.exception.CuentaBloqueadaException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
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

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional(noRollbackFor = { CredencialesInvalidasException.class, CuentaBloqueadaException.class })
    public LoginResponseDTO login(LoginRequestDTO request) {
        String identificador = request.getDocumento() != null && !request.getDocumento().isBlank()
                ? request.getDocumento().trim()
                : (request.getCorreo() != null ? request.getCorreo().trim() : null);

        if (identificador == null || identificador.isBlank()) {
            throw new BadRequestException("Debe ingresar un número de documento o correo electrónico");
        }

        // 1. Buscar usuario por número de documento o por correo (F-01, HU-001)
        Usuario usuario = usuarioRepository.findByDocumento(identificador)
                .or(() -> usuarioRepository.findByCorreo(identificador))
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "Número de documento o contraseña incorrectos"));

        // 2. Verificar si la cuenta está bloqueada o inactiva (F-08)
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada por seguridad. Contacte al Administrador");
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new CuentaBloqueadaException(
                    "La cuenta se encuentra INACTIVA. Contacte al Administrador");
        }

        // 3. Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            int intentosActuales = usuario.getIntentosFallidos() != null ? usuario.getIntentosFallidos() : 0;
            int nuevosIntentos = intentosActuales + 1;
            usuario.setIntentosFallidos((short) nuevosIntentos);

            if (nuevosIntentos >= 3) {
                usuario.setEstado(EstadoUsuario.BLOQUEADO);
                usuarioRepository.save(usuario);
                throw new CuentaBloqueadaException(
                        "Cuenta bloqueada por seguridad. Contacte al Administrador");
            }

            usuarioRepository.save(usuario);
            throw new CredencialesInvalidasException(
                    "Número de documento o contraseña incorrectos. Intentos fallidos: " + nuevosIntentos + "/3");
        }

        // 4. Reiniciar contador de intentos fallidos
        if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0) {
            usuario.setIntentosFallidos((short) 0);
            usuarioRepository.save(usuario);
        }

        // 5. Reiniciar la cuenta de inactividad (F-03) y generar el token con la versión vigente
        usuarioRepository.actualizarUltimaActividad(usuario.getId(), OffsetDateTime.now());
        int tokenVersion = usuario.getTokenVersion() != null ? usuario.getTokenVersion() : 0;
        String tokenReal = jwtProvider.generateToken(usuario.getDocumento(), tokenVersion);

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

    // Cierra la sesión invalidando todos los tokens emitidos al usuario (F-04)
    @Transactional
    public void cerrarSesion(String documento) {
        Usuario usuario = usuarioRepository.findByDocumento(documento)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuarioRepository.invalidarTokens(usuario.getId());
    }
}
