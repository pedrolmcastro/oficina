package com.oficina.api.aplicacao.ordemservico;

import java.util.UUID;

public record VeiculoDisponivelRetirada(UUID ordemId, String destinatario) {
    @Override
    public String toString() {
        return "VeiculoDisponivelRetirada[ordemId=" + this.ordemId + "]";
    }
}
