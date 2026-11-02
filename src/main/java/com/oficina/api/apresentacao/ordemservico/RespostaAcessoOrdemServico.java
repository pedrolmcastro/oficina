package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.aplicacao.ordemservico.AcessoOrdemServico;
import java.time.Instant;

public record RespostaAcessoOrdemServico(
    RespostaOrdemServico ordemServico,
    String tokenAcompanhamento,
    Instant expiraEm
) {
    public static RespostaAcessoOrdemServico de(final AcessoOrdemServico acesso) {
        return new RespostaAcessoOrdemServico(
            RespostaOrdemServico.de(acesso.ordemServico()),
            acesso.token(),
            acesso.ordemServico().tokenExpiraEm()
        );
    }
}
