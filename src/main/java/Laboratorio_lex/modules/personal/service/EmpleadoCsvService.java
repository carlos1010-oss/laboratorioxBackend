package Laboratorio_lex.modules.personal.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.ImportacionResultadoDTO;
import Laboratorio_lex.modules.personal.model.Departamento;
import Laboratorio_lex.modules.personal.model.EstadoEmpleado;
import Laboratorio_lex.modules.personal.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmpleadoCsvService {

    // Columnas de la plantilla estandarizada (F-13, F-14)
    private static final String[] CABECERAS = {
            "tipo_documento", "numero_documento", "nombres", "apellidos",
            "correo", "telefono", "codigo_departamento", "codigo_tarjeta_rfid",
            "estado", "motivo_cambio_estado"
    };

    // Límite de seguridad para no saturar memoria en archivos gigantes
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private static final Set<String> TIPOS_DOCUMENTO = Set.of("CC", "CE", "PASAPORTE", "PPT", "TI");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmpleadoService empleadoService;
    private final DepartamentoRepository departamentoRepository;

    // F-14: plantilla CSV estandarizada para la carga masiva
    public String generarPlantillaCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", CABECERAS)).append("\n");
        sb.append("CC,1020304050,Juan,Perez,juan.perez@laboratorioxyz.com,3001234567,DEP-PROD,RFID-001,ACTIVO,\n");
        sb.append("CE,1020304051,Maria,Gomez,maria.gomez@laboratorioxyz.com,3007654321,DEP-CAL,RFID-002,ACTIVO,\n");
        return sb.toString();
    }

    // F-13/F-15: carga masiva con validación de estructura, tipos y duplicados + reporte
    public ImportacionResultadoDTO importEmpleadosDesdeCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo CSV enviado está vacío.");
        }
        if (file.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new BadRequestException("El archivo supera el tamaño máximo permitido de 5 MB.");
        }

        List<String> errores = new ArrayList<>();
        int total = 0;
        int exitosos = 0;
        int fallidos = 0;
        Set<String> documentosEnArchivo = new HashSet<>();
        Set<String> tarjetasEnArchivo = new HashSet<>();

        // Compatibilidad real: Excel en español guarda con BOM UTF-8 y con ';'.
        // Se normaliza el contenido antes de parsear para no rechazar archivos válidos.
        String texto;
        try {
            texto = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BadRequestException("No fue posible leer el archivo CSV.");
        }
        if (!texto.isEmpty() && texto.charAt(0) == '\uFEFF') {
            texto = texto.substring(1);
        }
        String primeraLinea = texto.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        long puntoComas = primeraLinea.chars().filter(c -> c == ';').count();
        long comas = primeraLinea.chars().filter(c -> c == ',').count();
        char delimitador = puntoComas > comas ? ';' : ',';

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(texto));
                CSVParser parser = formato.parse(reader)) {

            Iterator<CSVRecord> iterador = parser.iterator();
            if (!iterador.hasNext()) {
                throw new BadRequestException("El archivo CSV no contiene encabezados.");
            }

            Map<String, Integer> columnas = mapearColumnas(iterador.next());

            // F-15: validar la estructura mínima antes de procesar
            List<String> faltantes = new ArrayList<>();
            for (String requerida : new String[] { "tipodocumento", "numerodocumento", "nombres", "apellidos" }) {
                if (!columnas.containsKey(requerida)) {
                    faltantes.add(requerida);
                }
            }
            if (!columnas.containsKey("codigodepartamento") && !columnas.containsKey("departamentoid")) {
                faltantes.add("codigo_departamento");
            }
            if (!faltantes.isEmpty()) {
                throw new BadRequestException("Faltan columnas obligatorias en el CSV: " + String.join(", ", faltantes));
            }

            while (iterador.hasNext()) {
                CSVRecord registro = iterador.next();
                total++;
                long linea = registro.getRecordNumber();

                try {
                    String tipoDocumento = mayusculas(valor(registro, columnas, "tipodocumento"));
                    String numeroDocumento = valor(registro, columnas, "numerodocumento");
                    String nombres = valor(registro, columnas, "nombres");
                    String apellidos = valor(registro, columnas, "apellidos");
                    String correo = valor(registro, columnas, "correo", "email");
                    String telefono = valor(registro, columnas, "telefono");
                    String codigoDepartamento = valor(registro, columnas, "codigodepartamento", "departamentoid");
                    String estadoStr = mayusculas(valor(registro, columnas, "estado"));
                    String motivo = valor(registro, columnas, "motivocambioestado", "motivo");
                    String tarjeta = valor(registro, columnas, "codigotarjetarfid", "rfid", "tarjeta");

                    // Validación de campos obligatorios
                    if (esVacio(tipoDocumento))
                        throw new IllegalArgumentException("El tipo de documento es obligatorio.");
                    if (esVacio(numeroDocumento))
                        throw new IllegalArgumentException("El número de documento es obligatorio.");
                    if (esVacio(nombres))
                        throw new IllegalArgumentException("Los nombres son obligatorios.");
                    if (esVacio(apellidos))
                        throw new IllegalArgumentException("Los apellidos son obligatorios.");
                    if (esVacio(codigoDepartamento))
                        throw new IllegalArgumentException("El departamento es obligatorio.");

                    if (!TIPOS_DOCUMENTO.contains(tipoDocumento)) {
                        throw new IllegalArgumentException(
                                "Tipo de documento inválido: " + tipoDocumento + " (permitidos: " + TIPOS_DOCUMENTO + ").");
                    }
                    if (!esVacio(correo) && !EMAIL.matcher(correo).matches()) {
                        throw new IllegalArgumentException("Correo con formato inválido: " + correo);
                    }

                    EstadoEmpleado estado = EstadoEmpleado.ACTIVO;
                    if (!esVacio(estadoStr)) {
                        try {
                            estado = EstadoEmpleado.valueOf(estadoStr);
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException(
                                    "Estado inválido: " + estadoStr + " (permitidos: ACTIVO, INACTIVO, BLOQUEADO).");
                        }
                    }
                    if (estado != EstadoEmpleado.ACTIVO && esVacio(motivo)) {
                        throw new IllegalArgumentException(
                                "El motivo de cambio de estado es obligatorio cuando el estado no es ACTIVO (F-17).");
                    }

                    // F-15: duplicados dentro del propio archivo
                    if (!documentosEnArchivo.add(numeroDocumento)) {
                        throw new IllegalArgumentException(
                                "Número de documento duplicado dentro del archivo: " + numeroDocumento);
                    }
                    if (!esVacio(tarjeta) && !tarjetasEnArchivo.add(tarjeta)) {
                        throw new IllegalArgumentException(
                                "Código de tarjeta duplicado dentro del archivo: " + tarjeta);
                    }

                    Departamento departamento = resolverDepartamento(codigoDepartamento);

                    EmpleadoRequestDTO dto = EmpleadoRequestDTO.builder()
                            .tipoDocumento(tipoDocumento)
                            .numeroDocumento(numeroDocumento)
                            .nombres(nombres)
                            .apellidos(apellidos)
                            .correo(esVacio(correo) ? null : correo)
                            .telefono(esVacio(telefono) ? null : telefono)
                            .departamentoId(departamento.getId())
                            .codigoTarjetaRfid(esVacio(tarjeta) ? null : tarjeta)
                            .estado(estado)
                            .motivoCambioEstado(esVacio(motivo) ? null : motivo)
                            .build();

                    // Duplicados contra la base de datos y demás validaciones los aplica el servicio
                    empleadoService.crearEmpleado(dto);
                    exitosos++;

                } catch (Exception e) {
                    fallidos++;
                    errores.add("Línea " + linea + ": " + e.getMessage());
                }
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Error al procesar la estructura del archivo CSV: " + e.getMessage());
        }

        return ImportacionResultadoDTO.builder()
                .totalProcesados(total)
                .exitosos(exitosos)
                .fallidos(fallidos)
                .errores(errores)
                .build();
    }

    // Normaliza los encabezados quitando guiones/espacios y pasando a minúsculas
    private Map<String, Integer> mapearColumnas(CSVRecord encabezado) {
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < encabezado.size(); i++) {
            String nombre = normalizar(encabezado.get(i));
            if (nombre != null && !nombre.isBlank()) {
                columnas.putIfAbsent(nombre, i);
            }
        }
        return columnas;
    }

    private String normalizar(String texto) {
        return texto == null ? null : texto.trim().toLowerCase().replace("_", "").replace(" ", "");
    }

    private String valor(CSVRecord registro, Map<String, Integer> columnas, String... claves) {
        for (String clave : claves) {
            Integer indice = columnas.get(clave);
            if (indice != null && indice < registro.size()) {
                String valor = registro.get(indice);
                if (valor != null && !valor.isBlank()) {
                    return valor.trim();
                }
            }
        }
        return null;
    }

    private Departamento resolverDepartamento(String valor) {
        Optional<Departamento> porCodigo = departamentoRepository.findByCodigo(valor);
        Departamento departamento = porCodigo.orElseGet(() -> {
            if (valor.matches("\\d+")) {
                return departamentoRepository.findById(Integer.parseInt(valor))
                        .orElseThrow(() -> new IllegalArgumentException("Departamento no encontrado: " + valor));
            }
            throw new IllegalArgumentException("Departamento no encontrado: " + valor);
        });

        if (Boolean.FALSE.equals(departamento.getActivo())) {
            throw new IllegalArgumentException("El departamento está inactivo: " + valor);
        }
        return departamento;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String mayusculas(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }
}
