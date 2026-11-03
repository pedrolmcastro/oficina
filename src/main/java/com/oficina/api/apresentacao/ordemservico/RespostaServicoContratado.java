package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.ServicoContratado;
import java.math.BigDecimal;
import java.util.UUID;

public record RespostaServicoContratado(
    UUID id,
    UUID servicoId,
    String nome,
    BigDecimal precoVenda,
    boolean requerDiagnostico
) {
    public static RespostaServicoContratado de(final ServicoContratado servico) {
        return new RespostaServicoContratado(
            servico.id(),
            servico.servicoId(),
            servico.nome().texto().valor(),
            servico.precoVenda().preco().valor(),
            servico.requerDiagnostico()
        );
    }
}
