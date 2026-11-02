package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.OrdemServico;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record RespostaAcompanhamento(
    String status,
    @Nullable
    String resultado,
    BigDecimal total,
    List<RespostaServicoContratadoAcompanhamento> servicos,
    List<RespostaInsumoNecessarioAcompanhamento> insumosNecessarios
) {
    public RespostaAcompanhamento {
        servicos = List.copyOf(servicos);
        insumosNecessarios = List.copyOf(insumosNecessarios);
    }

    public static RespostaAcompanhamento de(final OrdemServico ordem) {
        return new RespostaAcompanhamento(
            ordem.status().valor(),
            ordem.resultado(),
            ordem.total(),
            ordem.servicos().stream().map(RespostaServicoContratadoAcompanhamento::de).toList(),
            ordem.insumos().stream().map(RespostaInsumoNecessarioAcompanhamento::de).toList()
        );
    }
}
