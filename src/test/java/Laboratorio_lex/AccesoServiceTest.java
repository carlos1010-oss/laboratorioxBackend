package Laboratorio_lex;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.accesos.dto.RegistroAccesoRequestDTO;
import Laboratorio_lex.modules.accesos.dto.ResultadoAccesoResponseDTO;
import Laboratorio_lex.modules.accesos.model.ResultadoAcceso;
import Laboratorio_lex.modules.accesos.service.AccesoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccesoServiceTest {

    @Autowired
    private AccesoService accesoService;

    @Test
    @DisplayName("Debe conceder Acceso Maestro (VERDE) automáticamente a usuarios con rol ADMINISTRADOR")
    void testAccesoMaestroAdministrador() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("0000000001") // Documento del usuario semilla Admin
                .areaId(1)                     // Área ZR-LAB01
                .build();

        ResultadoAccesoResponseDTO response = accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent");

        assertNotNull(response);
        assertEquals(ResultadoAcceso.AUTORIZADO, response.getResultado());
        assertEquals("VERDE", response.getColor());
        assertNotNull(response.getNombreEmpleado());
        assertTrue(response.getNombreEmpleado().contains("Admin") || response.getNombreEmpleado().contains("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Debe retornar NO_REGISTRADO (AMARILLO) ante un documento que no existe")
    void testAccesoPersonaNoRegistrada() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("999999999999")
                .areaId(1)
                .build();

        ResultadoAccesoResponseDTO response = accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent");

        assertNotNull(response);
        assertEquals(ResultadoAcceso.NO_REGISTRADO, response.getResultado());
        assertEquals("AMARILLO", response.getColor());
        assertEquals("Persona no registrada en el sistema", response.getMotivo());
    }

    @Test
    @DisplayName("Debe validar soporte para campo alias 'documento'")
    void testAliasDocumentoEnDTO() {
        RegistroAccesoRequestDTO dto = new RegistroAccesoRequestDTO();
        dto.setDocumento("0000000001");
        dto.setAreaId(1);

        assertEquals("0000000001", dto.getNumeroDocumento());
        assertEquals("0000000001", dto.getDocumento());
    }

    @Test
    @DisplayName("Doble factor: debe exigir documento y tarjeta juntos en el canal público")
    void testDobleFactorExigeAmbos() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("0000000001")
                .areaId(1)
                .build();

        assertThrows(BadRequestException.class, () -> accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent", true));
    }

    @Test
    @DisplayName("Doble factor: combinación desconocida retorna NO_REGISTRADO")
    void testDobleFactorCombinacionDesconocida() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("999999999999")
                .codigoTarjetaRfid("TARJETA-INEXISTENTE")
                .areaId(1)
                .build();

        ResultadoAccesoResponseDTO response = accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent", true);

        assertNotNull(response);
        assertEquals(ResultadoAcceso.NO_REGISTRADO, response.getResultado());
        assertEquals("AMARILLO", response.getColor());
    }

    @Test
    @DisplayName("Canal interno con doble factor conserva acceso maestro del Administrador")
    void testDobleFactorInternoMantieneMaestro() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("0000000001")
                .codigoTarjetaRfid("TARJETA-INEXISTENTE")
                .areaId(1)
                .build();

        ResultadoAccesoResponseDTO response = accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent", true, true);

        assertNotNull(response);
        assertEquals(ResultadoAcceso.AUTORIZADO, response.getResultado());
    }

    @Test
    @DisplayName("Canal público con doble factor no aplica acceso maestro del Administrador")
    void testDobleFactorPublicoSinMaestro() {
        RegistroAccesoRequestDTO dto = RegistroAccesoRequestDTO.builder()
                .numeroDocumento("0000000001")
                .codigoTarjetaRfid("TARJETA-INEXISTENTE")
                .areaId(1)
                .build();

        ResultadoAccesoResponseDTO response = accesoService.procesarAccesoMolinete(
                dto, "127.0.0.1", "JUnit-Test-Agent", true, false);

        assertNotNull(response);
        assertEquals(ResultadoAcceso.NO_REGISTRADO, response.getResultado());
    }
}
