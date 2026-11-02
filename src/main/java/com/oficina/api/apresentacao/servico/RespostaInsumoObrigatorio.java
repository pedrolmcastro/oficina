package com.oficina.api.apresentacao.servico;

import com.oficina.api.dominio.servico.InsumoObrigatorio;
import java.util.UUID;

public record RespostaInsumoObrigatorio(UUID insumoId, int quantidade) {
    public static RespostaInsumoObrigatorio de(final InsumoObrigatorio obrigatorio) {
        return new RespostaInsumoObrigatorio(obrigatorio.insumoId(), obrigatorio.quantidade().valor());
    }
}
