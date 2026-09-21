package Laboratorio_lex;

import Laboratorio_lex.modules.auditoria.dto.AuditoriaRequestDTO;
import Laboratorio_lex.modules.auditoria.dto.AuditoriaResponseDTO;
import Laboratorio_lex.modules.auditoria.model.TipoOperacion;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditoriaServiceTest {

    @Autowired
    private AuditoriaService auditoriaService;

    @Test
    @DisplayName("Debe registrar y consultar eventos en la bitácora de auditoría inmutable")
    void testRegistroYConsultaAuditoria() {
        AuditoriaRequestDTO request = AuditoriaRequestDTO.builder()
                .usuarioId(1L)
                .direccionIp("192.168.1.50")
                .tipoOperacion(TipoOperacion.CREACION)
                .moduloTabla("departamentos")
                .valorAnterior(null)
                .valorNuevo("{\"codigo\":\"DEP-TEST\",\"nombre\":\"Departamento de Pruebas\"}")
                .build();

        assertDoesNotThrow(() -> auditoriaService.registrarEvento(request));

        Page<AuditoriaResponseDTO> resultados = auditoriaService.obtenerPaginado(
                1L, TipoOperacion.CREACION, "departamentos", null, null, 0, 10);

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty());
        AuditoriaResponseDTO primerRegistro = resultados.getContent().get(0);
        assertEquals(TipoOperacion.CREACION, primerRegistro.getTipoOperacion());
        assertEquals("departamentos", primerRegistro.getModuloTabla());
    }
}
