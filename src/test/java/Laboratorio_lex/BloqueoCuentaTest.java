package Laboratorio_lex;

import Laboratorio_lex.common.exception.CuentaBloqueadaException;
import Laboratorio_lex.config.JwtProvider;
import Laboratorio_lex.modules.auditoria.service.AuditoriaService;
import Laboratorio_lex.modules.auth.dto.LoginRequestDTO;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.TokenRecuperacionRepository;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Mi primera prueba UNITARIA: no usa base de datos, todo es de juguete (mocks).
@ExtendWith(MockitoExtension.class)
class BloqueoCuentaTest {

    // Actores de juguete: fingen ser la BD, el cifrador, etc.
    @Mock UsuarioRepository usuarioRepository;
    @Mock TokenRecuperacionRepository tokenRecuperacionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock AuditoriaService auditoriaService;

    // El protagonista real, con sus ayudantes de juguete enchufados
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("Al tercer fallo la cuenta se bloquea")
    void testBloqueoTercerIntento() {
        // 1. PREPARO: un usuario con 2 fallos previos
        Usuario usuario = Usuario.builder()
                .id(1L)
                .documento("123")
                .passwordHash("HASH")
                .estado(EstadoUsuario.ACTIVO)
                .intentosFallidos((short) 2)
                .build();

        // Les enseño a los juguetes cómo comportarse.
        // OJO: no programo findByCorreo porque el login solo lo llama si el
        // documento no existe (el .or() es perezoso). Mockito estricto prohíbe
        // juguetes programados que nadie usa.
        when(usuarioRepository.findByDocumento("123")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", "HASH")).thenReturn(false);

        // 2. EJECUTO y 3. COMPRUEBO: debe explotar con bloqueo
        assertThrows(CuentaBloqueadaException.class, () ->
                authService.login(LoginRequestDTO.builder()
                        .documento("123")
                        .password("clave-mala")
                        .build()));

        // 3b. Compruebo el estado final del usuario
        assertEquals(EstadoUsuario.BLOQUEADO, usuario.getEstado());
        assertEquals((short) 3, usuario.getIntentosFallidos());
    }
}
