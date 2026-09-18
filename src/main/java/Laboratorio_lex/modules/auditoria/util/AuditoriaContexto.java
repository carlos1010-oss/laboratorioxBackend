package Laboratorio_lex.modules.auditoria.util;

import Laboratorio_lex.modules.auth.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// F-31 / F-32: provee el usuario autenticado (JWT) y la IP real del cliente
// para registrar la bitácora de auditoría con trazabilidad real.
@Component
public class AuditoriaContexto {

    public Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }

    public String obtenerIpActual() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest request = attrs.getRequest();
                String ipEncadenada = request.getHeader("X-Forwarded-For");
                if (ipEncadenada != null && !ipEncadenada.isBlank()) {
                    return ipEncadenada.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // Sin request activo (jobs o pruebas unitarias): se asume local
        }
        return "127.0.0.1";
    }
}