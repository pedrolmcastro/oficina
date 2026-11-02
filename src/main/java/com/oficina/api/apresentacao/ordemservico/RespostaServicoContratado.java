package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.ServicoContratado;
import java.math.BigDecimal;
import java.util.UUID;

public record RespostaServicoContratado(
    UUID id,
    UUID servicoId,
    String nome,
    BigDecimal preco,
    boolean requerDiagnostico
) {
    public static RespostaServicoContratado de(final ServicoContratado servico) {
        return new RespostaServicoContratado(
            servico.id(),
            servico.servicoId(),
            servico.nome().texto().valor(),
            servico.preco().preco().valor(),
            servico.requerDiagnostico()
        );
    }
}
