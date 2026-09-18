package Laboratorio_lex.modules.portal.service;

import Laboratorio_lex.common.exception.BadRequestException;
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

    @Transactional(readOnly = true)
    public VerificacionIngresoDTO verificarRegistro(String documento, String tarjeta) {
        boolean tieneDocumento = documento != null && !documento.isBlank();
        boolean tieneTarjeta = tarjeta != null && !tarjeta.isBlank();

        if (!tieneDocumento && !tieneTarjeta) {
            throw new BadRequestException("Debe indicar el número de documento o el código de la tarjeta RFID");
        }

        Optional<Empleado> empleadoOpt = tieneDocumento
                ? empleadoRepository.findByNumeroDocumento(documento)
                : empleadoRepository.findByCodigoTarjetaRfid(tarjeta);

        if (empleadoOpt.isEmpty()) {
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