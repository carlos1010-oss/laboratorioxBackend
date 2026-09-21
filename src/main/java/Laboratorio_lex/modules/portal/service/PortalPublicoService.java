package Laboratorio_lex.modules.portal.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import Laboratorio_lex.modules.portal.dto.VerificacionIngresoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// F-35: consulta pública de registro. No expone datos personales, solo si la
// persona está registrada y su estado.
@Service
@RequiredArgsConstructor
public class PortalPublicoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public VerificacionIngresoDTO verificarRegistro(String documento, String tarjeta) {
        String doc = documento != null ? documento.trim() : null;
        String tar = tarjeta != null ? tarjeta.trim() : null;

        boolean tieneDocumento = doc != null && !doc.isBlank();
        boolean tieneTarjeta = tar != null && !tar.isBlank();

        if (!tieneDocumento && !tieneTarjeta) {
            throw new BadRequestException("Debe indicar el número de documento o el código de la tarjeta RFID");
        }

        Optional<Empleado> empleadoOpt = tieneDocumento
                ? empleadoRepository.findByNumeroDocumento(doc)
                : empleadoRepository.findByCodigoTarjetaRfid(tar);

        if (empleadoOpt.isEmpty()) {
            // Verificar si es un usuario del sistema (Administrador o Supervisor)
            String identificador = tieneDocumento ? doc : tar;
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDocumento(identificador)
                    .or(() -> usuarioRepository.findByCorreo(identificador));

            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                boolean activo = u.getEstado() == EstadoUsuario.ACTIVO;
                return VerificacionIngresoDTO.builder()
                        .registrado(true)
                        .estado(u.getEstado() != null ? u.getEstado().name() : null)
                        .autorizadoIngreso(activo)
                        .build();
            }

            return VerificacionIngresoDTO.builder()
                    .registrado(false)
                    .estado(null)
                    .autorizadoIngreso(false)
                    .build();
        }

        Empleado empleado = empleadoOpt.get();
        boolean activo = empleado.getEstado() == EstadoEmpleado.ACTIVO;

        return VerificacionIngresoDTO.builder()
                .registrado(true)
                .estado(empleado.getEstado() != null ? empleado.getEstado().name() : null)
                .autorizadoIngreso(activo)
                .build();
    }
}
