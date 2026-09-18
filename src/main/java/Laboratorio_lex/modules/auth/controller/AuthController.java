package Laboratorio_lex.modules.auth.controller;

import Laboratorio_lex.modules.auth.dto.*;
import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // Cierre de sesión: invalida el token vigente del usuario autenticado (F-04)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            authService.cerrarSesion(usuario.getDocumento());
        }
        return ResponseEntity.ok().build();
    }

    // F-02: Solicitud de token de recuperación de contraseña
    @PostMapping("/recuperar-password")
    public ResponseEntity<RecuperacionResponseDTO> solicitarRecuperacion(
            @Valid @RequestBody RecuperarPasswordDTO request) {
        return ResponseEntity.ok(authService.solicitarRecuperacion(request));
    }

    // F-02: Restablecimiento de contraseña con token
    @PostMapping("/reset-password")
    public ResponseEntity<Void> restablecerPassword(
            @Valid @RequestBody ResetPasswordDTO request) {
        authService.restablecerPassword(request);
        return ResponseEntity.ok().build();
    }
}
