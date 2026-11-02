package com.oficina.api.dominio.ordemservico;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public record EventoOrdemServico(
    int sequencia,
    String tipo,
    Instant ocorridoEm,
    String autor,
    @JdbcTypeCode(value = SqlTypes.JSON)
    Map<String, String> dados
) {
    public EventoOrdemServico {
        dados = Map.copyOf(dados);
    }
}
