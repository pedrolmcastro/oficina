package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.EventoOrdemServico;
import java.time.Instant;
import java.util.Map;

public record RespostaEventoOrdemServico(
    int sequencia,
    String tipo,
    Instant ocorridoEm,
    String autor,
    Map<String, String> dados
) {
    public RespostaEventoOrdemServico {
        dados = Map.copyOf(dados);
    }

    public static RespostaEventoOrdemServico de(final EventoOrdemServico evento) {
        return new RespostaEventoOrdemServico(
            evento.sequencia(),
            evento.tipo(),
            evento.ocorridoEm(),
            evento.autor(),
            evento.dados()
        );
    }
}
