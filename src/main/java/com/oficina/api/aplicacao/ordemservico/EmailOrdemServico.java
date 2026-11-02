package com.oficina.api.aplicacao.ordemservico;

import java.util.UUID;

public record EmailOrdemServico(UUID ordemId, String destinatario, String token, boolean decisao) {
    @Override
    public String toString() {
        return "EmailOrdemServico[ordemId=" + this.ordemId + "]";
    }
}
