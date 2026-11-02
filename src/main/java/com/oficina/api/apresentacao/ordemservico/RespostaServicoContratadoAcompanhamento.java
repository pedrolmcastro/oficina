package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.ServicoContratado;
import java.math.BigDecimal;

public record RespostaServicoContratadoAcompanhamento(String nome, BigDecimal preco) {
    public static RespostaServicoContratadoAcompanhamento de(final ServicoContratado servico) {
        return new RespostaServicoContratadoAcompanhamento(
            servico.nome().texto().valor(),
            servico.preco().preco().valor()
        );
    }
}
