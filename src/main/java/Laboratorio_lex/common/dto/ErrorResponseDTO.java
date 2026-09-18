package Laboratorio_lex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDTO {
    private int status;
    private String error;
    private String message;
    private String path;
    private OffsetDateTime timestamp;
    private Map<String, String> errors; // Contendrá los errores de validación de campos (@Valid)
}