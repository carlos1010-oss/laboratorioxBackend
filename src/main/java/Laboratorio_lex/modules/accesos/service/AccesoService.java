package Laboratorio_lex.modules.accesos.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.model.AreaRestringida;
import Laboratorio_lex.modules.accesos.model.HistorialAcceso;
import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import Laboratorio_lex.modules.accesos.repository.AreaRestringidaRepository;
import Laboratorio_lex.modules.accesos.repository.AutorizacionZonaRepository;
import Laboratorio_lex.modules.accesos.repository.HistorialAccesoRepository;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.personal.model.Empleado;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccesoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRestringidaRepository areaRestringidaRepository;
    private final AutorizacionZonaRepository autorizacionZonaRepository;
    private final HistorialAccesoRepository historialAccesoRepository;

    @Transactional
    public ResultadoAccesoResponseDTO procesarAccesoMolinete(RegistroAccesoRequestDTO dto, String ipOrigen,
            String userAgent) {

        String doc = dto.getNumeroDocumento() != null ? dto.getNumeroDocumento().trim() : null;
        String rfid = dto.getCodigoTarjetaRfid() != null ? dto.getCodigoTarjetaRfid().trim() : null;

        boolean tieneDocumento = doc != null && !doc.isBlank();
        boolean tieneTarjeta = rfid != null && !rfid.isBlank();

        if (!tieneDocumento && !tieneTarjeta) {
            throw new BadRequestException("Debe ingresar el número de documento o el código de la tarjeta RFID");
        }

        // 1. Validar existencia del área
        Optional<AreaRestringida> areaOpt = areaRestringidaRepository.findById(dto.getAreaId());
        if (areaOpt.isEmpty()) {
            return registrarYResponder(null, null, null, ResultadoAcceso.NO_REGISTRADO,
                    "Área restringida no encontrada con ID: " + dto.getAreaId(), dto, ipOrigen, userAgent);
        }
        AreaRestringida area = areaOpt.get();

        if (!Boolean.TRUE.equals(area.getActiva())) {
            return registrarYResponder(null, null, area, ResultadoAcceso.DENEGADO,
                    "El área restringida se encuentra inactiva", dto, ipOrigen, userAgent);
        }

        // 2. Validar existencia del empleado por documento o tarjeta (F-20)
        Optional<Empleado> empleadoOpt = tieneDocumento
                ? empleadoRepository.findByNumeroDocumento(doc)
                : empleadoRepository.findByCodigoTarjetaRfidIgnoreCase(rfid);

        // Si no está registrado como empleado operativo, verificar si es usuario del sistema (Administrador o Supervisor)
        if (empleadoOpt.isEmpty()) {
            String identificador = tieneDocumento ? doc : rfid;
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDocumento(identificador)
                    .or(() -> usuarioRepository.findByCorreo(identificador));

            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";

                if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
                    return registrarYResponder(null, usuario, area, ResultadoAcceso.DENEGADO,
                            "Usuario del sistema bloqueado o inactivo (" + usuario.getEstado() + ")", dto, ipOrigen, userAgent);
                }

                if ("ADMINISTRADOR".equalsIgnoreCase(rolNombre) || "SUPERVISOR_ACCESOS".equalsIgnoreCase(rolNombre)) {
                    // Acceso Maestro para Administradores y Supervisores
                    return registrarYResponder(null, usuario, area, ResultadoAcceso.AUTORIZADO,
                            "Acceso maestro autorizado (" + rolNombre + ")", dto, ipOrigen, userAgent);
                } else {
                    return registrarYResponder(null, usuario, area, ResultadoAcceso.DENEGADO,
                            "El rol del usuario (" + rolNombre + ") no tiene permisos de acceso a áreas de laboratorio",
                            dto, ipOrigen, userAgent);
                }
            }

            return registrarYResponder(null, null, area, ResultadoAcceso.NO_REGISTRADO,
                    "Persona no registrada en el sistema", dto, ipOrigen, userAgent);
        }

        Empleado empleado = empleadoOpt.get();

        // 3. Validar estado del empleado
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            return registrarYResponder(empleado, null, area, ResultadoAcceso.DENEGADO,
                    "Empleado inactivo/suspendido. Estado actual: " + empleado.getEstado(), dto, ipOrigen, userAgent);
        }

        // 4. Validar autorización de zona
        boolean tienePermiso = autorizacionZonaRepository.existsByEmpleadoIdAndAreaIdAndActivoTrue(empleado.getId(),
                area.getId());
        if (!tienePermiso) {
            return registrarYResponder(empleado, null, area, ResultadoAcceso.DENEGADO,
                    "El empleado no tiene autorización de acceso para esta área restringida", dto, ipOrigen, userAgent);
        }

        // 5. Acceso Autorizado
        return registrarYResponder(empleado, null, area, ResultadoAcceso.AUTORIZADO,
                "Acceso autorizado", dto, ipOrigen, userAgent);
    }

    // F-24: consulta filtrada y paginada del historial
    @Transactional(readOnly = true)
    public Page<ResultadoAccesoResponseDTO> buscarHistorial(String numeroDocumento, Integer areaId,
            OffsetDateTime fechaInicio, OffsetDateTime fechaFin, Pageable pageable) {
        Specification<HistorialAcceso> spec = HistorialSpecifications.conFiltros(numeroDocumento, areaId, fechaInicio,
                fechaFin);
        return historialAccesoRepository.findAll(spec, pageable).map(this::convertirADTO);
    }

    // F-25: listado completo (sin paginar) de los registros que cumplen los filtros, para exportación o cliente
    @Transactional(readOnly = true)
    public List<ResultadoAccesoResponseDTO> listarParaExportar(String numeroDocumento, Integer areaId,
            OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        Specification<HistorialAcceso> spec = HistorialSpecifications.conFiltros(numeroDocumento, areaId, fechaInicio,
                fechaFin);
        return historialAccesoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp")).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private ResultadoAccesoResponseDTO registrarYResponder(
            Empleado empleado,
            Usuario usuario,
            AreaRestringida area,
            ResultadoAcceso resultado,
            String motivo,
            RegistroAccesoRequestDTO dto,
            String ipOrigen,
            String userAgent) {

        String doc = dto.getNumeroDocumento();
        if (doc == null && usuario != null) {
            doc = usuario.getDocumento();
        }

        HistorialAcceso historial = HistorialAcceso.builder()
                .empleado(empleado)
                .area(area)
                .numeroDocumentoIngresado(doc)
                .codigoTarjetaIngresado(dto.getCodigoTarjetaRfid())
                .resultado(resultado)
                .motivoDenegacion(resultado != ResultadoAcceso.AUTORIZADO ? motivo : null)
                .ipOrigen(toInetAddress(ipOrigen))
                .userAgent(truncar(userAgent, 255))
                .timestamp(OffsetDateTime.now())
                .build();

        HistorialAcceso guardado = historialAccesoRepository.save(historial);
        ResultadoAccesoResponseDTO response = convertirADTO(guardado);

        // Si fue un usuario administrativo, enriquecer el nombre en la respuesta
        if (usuario != null && empleado == null) {
            String nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos() + " [" + usuario.getRol().getNombre() + "]";
            response.setNombreEmpleado(nombreAdmin);
        }

        if (motivo != null) {
            response.setMotivo(motivo);
        }

        return response;
    }

    private ResultadoAccesoResponseDTO convertirADTO(HistorialAcceso h) {
        String nombreEmp;
        if (h.getEmpleado() != null) {
            nombreEmp = h.getEmpleado().getNombres() + " " + h.getEmpleado().getApellidos();
        } else if (h.getNumeroDocumentoIngresado() != null && !h.getNumeroDocumentoIngresado().isBlank()) {
            nombreEmp = usuarioRepository.findByDocumento(h.getNumeroDocumentoIngresado())
                    .map(u -> u.getNombres() + " " + u.getApellidos() + " [" + u.getRol().getNombre() + "]")
                    .orElse("DESCONOCIDO");
        } else {
            nombreEmp = "DESCONOCIDO";
        }

        String estadoEmp = (h.getEmpleado() != null && h.getEmpleado().getEstado() != null)
                ? h.getEmpleado().getEstado().name()
                : "NO_REGISTRADO";

        String nombreArea = (h.getArea() != null) ? h.getArea().getNombre() : "N/A";

        return ResultadoAccesoResponseDTO.builder()
                .idHistorial(h.getId())
                .numeroDocumentoIngresado(h.getNumeroDocumentoIngresado())
                .codigoTarjetaRfid(h.getCodigoTarjetaIngresado())
                .nombreEmpleado(nombreEmp)
                .estadoEmpleado(estadoEmp)
                .nombreArea(nombreArea)
                .resultado(h.getResultado())
                .color(colorDe(h.getResultado()))
                .motivo(h.getMotivoDenegacion())
                .fechaHora(h.getTimestamp())
                .ipOrigen(h.getIpOrigen() != null ? h.getIpOrigen().getHostAddress() : null)
                .userAgent(h.getUserAgent())
                .build();
    }

    // F-22: semáforo visual del resultado
    private String colorDe(ResultadoAcceso resultado) {
        if (resultado == null) {
            return "GRIS";
        }
        return switch (resultado) {
            case AUTORIZADO -> "VERDE";
            case DENEGADO -> "ROJO";
            case NO_REGISTRADO -> "AMARILLO";
        };
    }

    private InetAddress toInetAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
