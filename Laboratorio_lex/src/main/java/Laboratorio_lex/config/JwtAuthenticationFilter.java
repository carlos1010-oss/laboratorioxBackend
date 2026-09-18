package Laboratorio_lex.config;

import Laboratorio_lex.modules.auth.model.EstadoUsuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UsuarioRepository usuarioRepository;

    // Minutos de inactividad permitidos antes de invalidar la sesión (F-03)
    @Value("${seguridad.sesion.inactividad-minutos:5}")
    private long inactividadMinutos;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtProvider.validateToken(token)) {
                String documento = jwtProvider.getSubjectFromToken(token);
                int tokenVersion = jwtProvider.getTokenVersionFromToken(token);

                usuarioRepository.findByDocumento(documento).ifPresent(usuario -> {
                    if (sesionVigente(usuario, tokenVersion)) {
                        var authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre());
                        var authentication = new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                Collections.singletonList(authority));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Registra la actividad para reiniciar la cuenta de inactividad (F-03)
                        usuarioRepository.actualizarUltimaActividad(usuario.getId(), OffsetDateTime.now());
                    }
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    // Comprueba estado, versión del token y vigencia por inactividad (F-03, F-04, F-08)
    private boolean sesionVigente(Laboratorio_lex.modules.auth.model.Usuario usuario, int tokenVersion) {
        if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getRol() == null) {
            return false;
        }
        if (usuario.getTokenVersion() == null || usuario.getTokenVersion() != tokenVersion) {
            return false;
        }
        if (usuario.getUltimaActividad() != null) {
            long minutosInactivo = Duration.between(usuario.getUltimaActividad(), OffsetDateTime.now()).toMinutes();
            if (minutosInactivo >= inactividadMinutos) {
                return false;
            }
        }
        return true;
    }
}