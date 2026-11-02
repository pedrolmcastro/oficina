package com.oficina.api.dominio.ordemservico;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusOrdemServico {
    AGUARDANDO_DIAGNOSTICO,
    EM_DIAGNOSTICO,
    AGUARDANDO_CONFIRMACAO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    AGUARDANDO_RETIRADA,
    ENTREGUE;

    @JsonValue
    public String valor() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
