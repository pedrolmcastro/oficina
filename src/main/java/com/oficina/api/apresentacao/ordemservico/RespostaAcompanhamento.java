package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.OrdemServico;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record RespostaAcompanhamento(
    String status,
    @Nullable
    String resultado,
    BigDecimal orcamento,
    List<RespostaServicoContratadoAcompanhamento> servicosContratados,
    List<RespostaInsumoNecessarioAcompanhamento> insumosNecessarios
) {
    public RespostaAcompanhamento {
        servicosContratados = List.copyOf(servicosContratados);
        insumosNecessarios = List.copyOf(insumosNecessarios);
    }

    public static RespostaAcompanhamento de(final OrdemServico ordem) {
        return new RespostaAcompanhamento(
            ordem.status().valor(),
            ordem.resultado(),
            ordem.orcamento(),
            ordem.servicosContratados().stream().map(RespostaServicoContratadoAcompanhamento::de).toList(),
            ordem.insumosNecessarios().stream().map(RespostaInsumoNecessarioAcompanhamento::de).toList()
        );
    }
}
