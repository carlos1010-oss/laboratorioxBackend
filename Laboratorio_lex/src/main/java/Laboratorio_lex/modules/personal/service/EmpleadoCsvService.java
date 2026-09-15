package Laboratorio_lex.modules.personal.service;

import Laboratorio_lex.common.exception.BadRequestException;
import Laboratorio_lex.modules.personal.dto.EmpleadoRequestDTO;
import Laboratorio_lex.modules.personal.dto.ImportacionResultadoDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpleadoCsvService {

    private final EmpleadoService empleadoService;

    public ImportacionResultadoDTO importEmpleadosDesdeCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("El archivo CSV enviado está vacío.");
        }

        List<String> errores = new ArrayList<>();
        int total = 0;
        int exitosos = 0;
        int fallidos = 0;

        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader("tipoDocumento", "numeroDocumento", "nombres", "apellidos", "correo", "telefono",
                                "departamentoId", "codigoTarjetaRfid")
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build())) {

            for (CSVRecord csvRecord : csvParser) {
                total++;
                try {
                    String tipoDoc = csvRecord.get("tipoDocumento");
                    String numDoc = csvRecord.get("numeroDocumento");
                    String nombres = csvRecord.get("nombres");
                    String apellidos = csvRecord.get("apellidos");
                    String correo = csvRecord.get("correo");
                    String telefono = csvRecord.get("telefono");
                    String deptoIdStr = csvRecord.get("departamentoId");
                    String rfid = csvRecord.get("codigoTarjetaRfid");

                    if (deptoIdStr == null || deptoIdStr.isBlank()) {
                        throw new IllegalArgumentException("El ID del departamento es requerido.");
                    }

                    Integer departamentoId = Integer.parseInt(deptoIdStr);

                    EmpleadoRequestDTO dto = EmpleadoRequestDTO.builder()
                            .tipoDocumento(tipoDoc)
                            .numeroDocumento(numDoc)
                            .nombres(nombres)
                            .apellidos(apellidos)
                            .correo(correo)
                            .telefono(telefono)
                            .departamentoId(departamentoId)
                            .codigoTarjetaRfid(rfid.isBlank() ? null : rfid)
                            .build();

                    empleadoService.crearEmpleado(dto);
                    exitosos++;

                } catch (Exception e) {
                    fallidos++;
                    errores.add("Línea " + csvRecord.getRecordNumber() + ": Error - " + e.getMessage());
                }
            }

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
}