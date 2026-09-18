package Laboratorio_lex.modules.sincronizacion.service;

import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import Laboratorio_lex.modules.accesos.repository.HistorialAccesoRepository;
import Laboratorio_lex.modules.sincronizacion.dto.ConsolidadoDepartamentoDTO;
import Laboratorio_lex.modules.sincronizacion.dto.ConsolidadoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

// F-26: consolidación periódica de la actividad de accesos agrupada por departamento
@Service
@RequiredArgsConstructor
public class ConsolidadoService {

    private final HistorialAccesoRepository historialAccesoRepository;

    @Transactional(readOnly = true)
    public ConsolidadoResponseDTO generarConsolidado(OffsetDateTime inicio, OffsetDateTime fin) {
        List<ConsolidadoDepartamentoDTO> porDepartamento = historialAccesoRepository
                .consolidarPorDepartamento(inicio, fin, ResultadoAcceso.AUTORIZADO, ResultadoAcceso.DENEGADO,
                        ResultadoAcceso.NO_REGISTRADO)
                .stream()
                .map(p -> ConsolidadoDepartamentoDTO.builder()
                        .departamentoId(p.getDepartamentoId())
                        .codigo(p.getCodigo())
                        .nombre(p.getNombre())
                        .totalIntentos(nz(p.getTotalIntentos()))
                        .autorizados(nz(p.getAutorizados()))
                        .denegados(nz(p.getDenegados()))
                        .noRegistrados(nz(p.getNoRegistrados()))
                        .build())
                .collect(Collectors.toList());

        long total = historialAccesoRepository.countByTimestampGreaterThanEqualAndTimestampLessThan(inicio, fin);
        long autorizados = historialAccesoRepository
                .countByResultadoAndTimestampGreaterThanEqualAndTimestampLessThan(ResultadoAcceso.AUTORIZADO, inicio,
                        fin);
        long denegados = historialAccesoRepository
                .countByResultadoAndTimestampGreaterThanEqualAndTimestampLessThan(ResultadoAcceso.DENEGADO, inicio,
                        fin);
        long noRegistrados = historialAccesoRepository
                .countByResultadoAndTimestampGreaterThanEqualAndTimestampLessThan(ResultadoAcceso.NO_REGISTRADO, inicio,
                        fin);

        return ConsolidadoResponseDTO.builder()
                .periodoInicio(inicio)
                .periodoFin(fin)
                .totalIntentos(total)
                .totalAutorizados(autorizados)
                .totalDenegados(denegados)
                .totalNoRegistrados(noRegistrados)
                .porDepartamento(porDepartamento)
                .build();
    }

    private long nz(Long valor) {
        return valor == null ? 0L : valor;
    }
}
