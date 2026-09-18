package Laboratorio_lex.modules.auth.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.CredencialesInvalidasException;
import Laboratorio_lex.common.exception.CuentaBloqueadaException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.config.JwtProvider;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auth.dto.*;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.TokenRecuperacion;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.TokenRecuperacionRepository;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuditoriaService auditoriaService;

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

                // Auditoría del bloqueo automático por seguridad (NF-10)
                registrarAuditoriaSeguridad(usuario, TipoOperacion.BLOQUEO,
                        "Cuenta bloqueada automáticamente tras 3 intentos fallidos consecutivos");

                throw new CuentaBloqueadaException(
                        "Cuenta bloqueada por seguridad tras 3 intentos fallidos. Contacte al Administrador");
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
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : "OPERADOR")
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

    // F-02: Solicitar recuperación de contraseña (genera token válido por 1 hora en tokens_recuperacion)
    @Transactional
    public RecuperacionResponseDTO solicitarRecuperacion(RecuperarPasswordDTO dto) {
        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(dto.getCorreo().trim());

        // Por seguridad anti-enumeración, se responde con éxito incluso si no existe
        if (userOpt.isEmpty()) {
            return RecuperacionResponseDTO.builder()
                    .mensaje("Si el correo electrónico existe en nuestra base de datos, se ha generado el enlace de recuperación.")
                    .tokenDemo(null)
                    .build();
        }

        Usuario usuario = userOpt.get();
        String rawToken = UUID.randomUUID().toString();

        TokenRecuperacion tokenRecuperacion = TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(rawToken)
                .expiracion(OffsetDateTime.now().plusHours(1))
                .usado(false)
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRecuperacionRepository.save(tokenRecuperacion);

        registrarAuditoriaSeguridad(usuario, TipoOperacion.MODIFICACION,
                "Solicitud de token de recuperación de contraseña");

        return RecuperacionResponseDTO.builder()
                .mensaje("Enlace de recuperación generado satisfactoriamente (válido por 60 minutos).")
                .tokenDemo(rawToken)
                .build();
    }

    // F-02: Restablecer contraseña utilizando el token generado
    @Transactional
    public void restablecerPassword(ResetPasswordDTO dto) {
        TokenRecuperacion token = tokenRecuperacionRepository
                .findByTokenHashAndUsadoFalseAndExpiracionAfter(dto.getToken().trim(), OffsetDateTime.now())
                .orElseThrow(() -> new BadRequestException(
                        "El token de recuperación es inválido, ya fue utilizado o ha expirado"));

        Usuario usuario = token.getUsuario();

        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setIntentosFallidos((short) 0);

        // Si la cuenta estaba bloqueada, reactivarla
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }

        usuarioRepository.save(usuario);
        usuarioRepository.invalidarTokens(usuario.getId());

        // Marcar token como usado
        token.setUsado(true);
        tokenRecuperacionRepository.save(token);

        registrarAuditoriaSeguridad(usuario, TipoOperacion.DESBLOQUEO,
                "Restablecimiento exitoso de contraseña mediante token de recuperación");
    }

    private void registrarAuditoriaSeguridad(Usuario usuario, TipoOperacion operacion, String detalle) {
        try {
            AuditoriaRequestDTO auditoriaDTO = AuditoriaRequestDTO.builder()
                    .usuarioId(usuario != null ? usuario.getId() : null)
                    .direccionIp("127.0.0.1")
                    .tipoOperacion(operacion)
                    .moduloTabla("usuarios")
                    .valorAnterior(null)
                    .valorNuevo("{\"detalle\":\"" + detalle + "\",\"usuario\":\"" + (usuario != null ? usuario.getCorreo() : "") + "\"}")
                    .build();

            auditoriaService.registrarEvento(auditoriaDTO);
        } catch (Exception e) {
            System.err.println("Error registrando auditoría de seguridad: " + e.getMessage());
        }
    }
}
