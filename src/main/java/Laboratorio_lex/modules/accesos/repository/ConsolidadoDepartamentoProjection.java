package Laboratorio_lex.modules.accesos.repository;

// F-26: proyección del consolidado de accesos agrupado por departamento
public interface ConsolidadoDepartamentoProjection {

    Integer getDepartamentoId();

    String getCodigo();

    String getNombre();

    Long getTotalIntentos();

    Long getAutorizados();

    Long getDenegados();

    Long getNoRegistrados();
}
