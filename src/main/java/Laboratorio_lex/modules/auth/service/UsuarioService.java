package Laboratorio_lex.modules.auth.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.common.exception.ResourceNotFoundException;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auditoria.util.AuditoriaContexto;
import Laboratorio_lex.modules.auth.dto.ResetPasswordAdminDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioCreacionDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioModificacionDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioResponseDTO;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Rol;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.RolRepository;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final AuditoriaContexto auditoriaContexto;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        return convertirADTO(buscar(id));
    }

    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioCreacionDTO dto) {
        if (usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new BadRequestException("Ya existe un usuario registrado con el documento: " + dto.getDocumento());
        }

        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new BadRequestException("Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getRolId()));

        Usuario usuario = Usuario.builder()
                .documento(dto.getDocumento())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .correo(dto.getCorreo())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .estado(EstadoUsuario.ACTIVO)
                .intentosFallidos((short) 0)
                .rol(rol)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        UsuarioResponseDTO response = convertirADTO(guardado);

        registrarAuditoria(null, response, TipoOperacion.CREACION);

        return response;
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioModificacionDTO dto) {
        Usuario usuario = buscar(id);
        UsuarioResponseDTO estadoAnterior = convertirADTO(usuario);

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getRolId()));

        // F-10: no degradar al único Administrador activo
        if (esUltimoAdministradorActivo(usuario) && !"ADMINISTRADOR".equals(rol.getNombre())) {
            throw new BadRequestException(
                    "No es posible cambiar el rol del único Administrador activo en el sistema");
        }

        if (!usuario.getCorreo().equals(dto.getCorreo()) && usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new BadRequestException("Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }

        usuario.setNombres(dto.getNombres());
        usuario.setApellidos(dto.getApellidos());
        usuario.setCorreo(dto.getCorreo());
        usuario.setRol(rol);

        Usuario actualizado = usuarioRepository.save(usuario);
        UsuarioResponseDTO estadoNuevo = convertirADTO(actualizado);

        registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

        return estadoNuevo;
    }

    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id, EstadoUsuario nuevoEstado) {
        Usuario usuario = buscar(id);
        UsuarioResponseDTO estadoAnterior = convertirADTO(usuario);

        // F-10: no bloquear/desactivar al único Administrador activo
        if (nuevoEstado != EstadoUsuario.ACTIVO && esUltimoAdministradorActivo(usuario)) {
            throw new BadRequestException(
                    "No es posible cambiar el estado del único Administrador activo en el sistema");
        }

        usuario.setEstado(nuevoEstado);

        // Si se desbloquea manualmente, se reinicia el contador de intentos fallidos
        if (nuevoEstado == EstadoUsuario.ACTIVO) {
            usuario.setIntentosFallidos((short) 0);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        UsuarioResponseDTO estadoNuevo = convertirADTO(actualizado);

        TipoOperacion operacion = (nuevoEstado == EstadoUsuario.BLOQUEADO)
                ? TipoOperacion.BLOQUEO
                : (nuevoEstado == EstadoUsuario.ACTIVO ? TipoOperacion.DESBLOQUEO : TipoOperacion.MODIFICACION);

        registrarAuditoria(estadoAnterior, estadoNuevo, operacion);

        return estadoNuevo;
    }

    // Reseteo administrativo de contraseña
    @Transactional
    public UsuarioResponseDTO restablecerPasswordPorAdmin(Long id, ResetPasswordAdminDTO dto) {
        Usuario usuario = buscar(id);
        UsuarioResponseDTO estadoAnterior = convertirADTO(usuario);

        usuario.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setIntentosFallidos((short) 0);
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        usuarioRepository.invalidarTokens(usuario.getId());

        UsuarioResponseDTO estadoNuevo = convertirADTO(actualizado);
        registrarAuditoria(estadoAnterior, estadoNuevo, TipoOperacion.MODIFICACION);

        return estadoNuevo;
    }

    private boolean esUltimoAdministradorActivo(Usuario usuario) {
        if (!"ADMINISTRADOR".equals(usuario.getRol().getNombre())
                || usuario.getEstado() != EstadoUsuario.ACTIVO) {
            return false;
        }
        return usuarioRepository.countByRol_NombreAndEstado("ADMINISTRADOR", EstadoUsuario.ACTIVO) <= 1;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    private void registrarAuditoria(Object anterior, Object nuevo, TipoOperacion operacion) {
        try {
            String jsonAnterior = anterior != null ? objectMapper.writeValueAsString(anterior) : null;
            String jsonNuevo = nuevo != null ? objectMapper.writeValueAsString(nuevo) : null;

            Usuario usuarioActual = auditoriaContexto.obtenerUsuarioActual();

            AuditoriaRequestDTO auditoriaDTO = AuditoriaRequestDTO.builder()
                    .usuarioId(usuarioActual != null ? usuarioActual.getId() : null)
                    .direccionIp(auditoriaContexto.obtenerIpActual())
                    .tipoOperacion(operacion)
                    .moduloTabla("usuarios")
                    .valorAnterior(jsonAnterior)
                    .valorNuevo(jsonNuevo)
                    .build();

            auditoriaService.registrarEvento(auditoriaDTO);
        } catch (Exception e) {
            System.err.println("Error al registrar evento de auditoría en usuarios: " + e.getMessage());
        }
    }

    private UsuarioResponseDTO convertirADTO(Usuario u) {
        return UsuarioResponseDTO.builder()
                .id(u.getId())
                .documento(u.getDocumento())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .correo(u.getCorreo())
                .estado(u.getEstado().name())
                .rol(u.getRol().getNombre())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
