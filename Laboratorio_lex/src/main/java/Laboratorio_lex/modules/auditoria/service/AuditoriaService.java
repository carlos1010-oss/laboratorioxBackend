package Laboratorio_lex.modules.auditoria.service;

import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaResponseDTO;
import Laboratorio_lex.modules.auditoria.model.BitacoraAuditoria;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.repository.AuditoriaSpecifications;
import Laboratorio_lex.modules.auditoria.repository.BitacoraAuditoriaRepository;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final BitacoraAuditoriaRepository bitacoraAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrarEvento(AuditoriaRequestDTO dto) {
        Usuario usuario = null;
        if (dto.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(dto.getUsuarioId()).orElse(null);
        }

        BitacoraAuditoria bitacora = BitacoraAuditoria.builder()
                .usuario(usuario)
                .direccionIp(dto.getDireccionIp())
                .tipoOperacion(dto.getTipoOperacion())
                .moduloTabla(dto.getModuloTabla())
                .valorAnterior(dto.getValorAnterior())
                .valorNuevo(dto.getValorNuevo())
                .build();

        bitacoraAuditoriaRepository.save(bitacora);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> obtenerPaginado(
            Long usuarioId,
            TipoOperacion tipoOperacion,
            String moduloTabla,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int page,
            int size) {

        OffsetDateTime inicio = fechaInicio != null
                ? fechaInicio.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
                : null;
        OffsetDateTime fin = fechaFin != null
                ? fechaFin.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault()).toOffsetDateTime()
                : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return bitacoraAuditoriaRepository
                .findAll(AuditoriaSpecifications.conFiltros(
                        usuarioId, tipoOperacion, moduloTabla, inicio, fin), pageable)
                .map(this::convertirADTO);
    }

    private AuditoriaResponseDTO convertirADTO(BitacoraAuditoria b) {
        String nombreUser = (b.getUsuario() != null)
                ? b.getUsuario().getNombres() + " " + b.getUsuario().getApellidos()
                : "SISTEMA";

        return AuditoriaResponseDTO.builder()
                .id(b.getId())
                .timestamp(b.getTimestamp())
                .usuarioId(b.getUsuario() != null ? b.getUsuario().getId() : null)
                .nombreUsuario(nombreUser)
                .direccionIp(b.getDireccionIp())
                .tipoOperacion(b.getTipoOperacion())
                .moduloTabla(b.getModuloTabla())
                .valorAnterior(b.getValorAnterior())
                .valorNuevo(b.getValorNuevo())
                .build();
    }
}
