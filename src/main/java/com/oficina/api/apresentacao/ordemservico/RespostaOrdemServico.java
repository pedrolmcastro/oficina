package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.OrdemServico;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaOrdemServico(
    UUID id,
    UUID clienteId,
    UUID veiculoId,
    String status,
    @Nullable
    String resultado,
    Instant criadaEm,
    BigDecimal total,
    List<RespostaServicoContratado> servicos,
    List<RespostaInsumoNecessario> insumosNecessarios,
    List<RespostaEventoOrdemServico> eventos
) {
    public RespostaOrdemServico {
        servicos = List.copyOf(servicos);
        insumosNecessarios = List.copyOf(insumosNecessarios);
        eventos = List.copyOf(eventos);
    }

    public static RespostaOrdemServico de(final OrdemServico ordem) {
        return new RespostaOrdemServico(
            ordem.id(),
            ordem.clienteId(),
            ordem.veiculoId(),
            ordem.status().valor(),
            ordem.resultado(),
            ordem.criadaEm(),
            ordem.total(),
            ordem.servicos().stream().map(RespostaServicoContratado::de).toList(),
            ordem.insumos().stream().map(RespostaInsumoNecessario::de).toList(),
            ordem.eventos().stream().map(RespostaEventoOrdemServico::de).toList()
        );
    }
}
