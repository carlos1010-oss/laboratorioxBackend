package Laboratorio_lex.modules.auth.controller;

import Laboratorio_lex.modules.auth.dto.ResetPasswordAdminDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioCreacionDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioModificacionDTO;
import Laboratorio_lex.modules.auth.dto.UsuarioResponseDTO;
import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioCreacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioModificacionDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoUsuario nuevoEstado) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, nuevoEstado));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<UsuarioResponseDTO> restablecerPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordAdminDTO dto) {
        return ResponseEntity.ok(usuarioService.restablecerPasswordPorAdmin(id, dto));
    }
}
