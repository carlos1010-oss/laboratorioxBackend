package Laboratorio_lex.modules.sincronizacion.service;

import Laboratorio_lex.modules.auth.model.Usuario;
import Laboratorio_lex.modules.auth.repository.UsuarioRepository;
import Laboratorio_lex.modules.sincronizacion.model.ConfiguracionExportacion;
import Laboratorio_lex.modules.sincronizacion.model.RegistroSincronizacionSocio;
import Laboratorio_lex.modules.sincronizacion.repository.ConfiguracionExportacionRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// F-29: alerta por correo a supervisores cuando un envío al socio queda en FALLIDO
@Service
@RequiredArgsConstructor
public class AlertaCorreoService {

    private static final Logger log = LoggerFactory.getLogger(AlertaCorreoService.class);

    // JavaMailSender sólo es bean si está configurado spring.mail.host; se inyecta de forma opcional
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionExportacionRepository configuracionRepository;

    @Value("${integracion.socio.correo-habilitado:false}")
    private boolean correoHabilitado;

    public void enviarAlertaSupervisor(RegistroSincronizacionSocio registro) {
        String destinatarios = resolverDestinatarios();
        if (destinatarios == null || destinatarios.isBlank()) {
            log.warn("No hay destinatarios configurados para la alerta de sincronización (ID registro {})",
                    registro.getId());
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!correoHabilitado || mailSender == null) {
            // Modo simulación: la integración SMTP real depende de credenciales del socio
            log.warn("Alerta de correo DESHABILITADA (integracion.socio.correo-habilitado=false). Destinatarios: {}",
                    destinatarios);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setTo(destinatarios.split(","));
            helper.setSubject("[Zone Control] FALLO en sincronización hacia el socio externo");
            helper.setText("Registro de sincronización fallido.\n\n"
                    + "ID: " + registro.getId() + "\n"
                    + "Periodo: " + registro.getPeriodoInicio() + " / " + registro.getPeriodoFin() + "\n"
                    + "Intentos realizados: " + registro.getIntentosRealizados() + "\n"
                    + "Código HTTP: " + registro.getCodigoRespuestaHttp() + "\n"
                    + "Fecha de intento: " + registro.getFechaEnvio() + "\n\n"
                    + "Revise la conectividad con el endpoint del socio y reenvíe manualmente.");
            mailSender.send(mensaje);
            log.info("Alerta enviada a {} por el fallo del registro {}", destinatarios, registro.getId());
        } catch (Exception e) {
            log.error("Error al enviar la alerta por correo: {}", e.getMessage());
        }
    }

    private String resolverDestinatarios() {
        ConfiguracionExportacion config = configuracionRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config != null && config.getCorreoAlerta() != null && !config.getCorreoAlerta().isBlank()) {
            return config.getCorreoAlerta();
        }
        List<String> correos = usuarioRepository.findByRol_Nombre("SUPERVISOR_ACCESOS").stream()
                .map(Usuario::getCorreo)
                .collect(Collectors.toList());
        return correos.isEmpty() ? null : String.join(",", correos);
    }
}