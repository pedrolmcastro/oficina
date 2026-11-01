package com.oficina.api.apresentacao.servico;

import com.oficina.api.dominio.servico.Servico;
import java.math.BigDecimal;
import java.util.UUID;

public record RespostaServico(UUID id, String nome, BigDecimal precoVenda, boolean requerDiagnostico) {
    public static RespostaServico de(final Servico servico) {
        return new RespostaServico(
            servico.id(),
            servico.nome().texto().valor(),
            servico.precoVenda().preco().valor(),
            servico.requerDiagnostico()
        );
    }
}
