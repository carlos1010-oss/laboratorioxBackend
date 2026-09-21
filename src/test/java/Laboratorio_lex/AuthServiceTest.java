package Laboratorio_lex;

import Laboratorio_lex.common.exception.CredencialesInvalidasException;
import Laboratorio_lex.common.exception.CuentaBloqueadaException;
import Laboratorio_lex.modules.auth.dto.*;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Debe autenticar exitosamente por documento con credenciales válidas")
    void testLoginPorDocumentoExitoso() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .documento("0000000001")
                .password("Admin123!")
                .build();

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals("0000000001", response.getUsuario().getDocumento());
        assertEquals("ADMINISTRADOR", response.getUsuario().getRol());
    }

    @Test
    @DisplayName("Debe autenticar exitosamente por correo electrónico con credenciales válidas")
    void testLoginPorCorreoExitoso() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .correo("admin@laboratorioxyz.com")
                .password("senafactory*")
                .build();

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin@laboratorioxyz.com", response.getUsuario().getCorreo());
    }

    @Test
    @DisplayName("Debe lanzar excepción ante contraseña incorrecta e incrementar intentos fallidos")
    void testPasswordIncorrecta() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .documento("0000000001")
                .password("ContrasenaTotalmenteInvalida123!")
                .build();

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));

        Usuario usuario = usuarioRepository.findByDocumento("0000000001").orElseThrow();
        assertTrue(usuario.getIntentosFallidos() >= 1);
    }

    @Test
    @DisplayName("Debe generar token de recuperación y permitir restablecer la contraseña")
    void testFlujoRecuperacionPassword() {
        // 1. Solicitar token
        RecuperarPasswordDTO recuperarDTO = RecuperarPasswordDTO.builder()
                .correo("admin@laboratorioxyz.com")
                .build();

        RecuperacionResponseDTO recResponse = authService.solicitarRecuperacion(recuperarDTO);
        assertNotNull(recResponse);
        assertNotNull(recResponse.getTokenDemo());

        // 2. Restablecer contraseña con el token generado
        ResetPasswordDTO resetDTO = ResetPasswordDTO.builder()
                .token(recResponse.getTokenDemo())
                .nuevaPassword("NuevaPasswordSegura2026*")
                .build();

        assertDoesNotThrow(() -> authService.restablecerPassword(resetDTO));

        // 3. Login con la nueva contraseña
        LoginRequestDTO loginNuevo = LoginRequestDTO.builder()
                .correo("admin@laboratorioxyz.com")
                .password("NuevaPasswordSegura2026*")
                .build();

        LoginResponseDTO loginResponse = authService.login(loginNuevo);
        assertNotNull(loginResponse.getToken());
    }
}
