package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.InsumoNecessario;
import java.math.BigDecimal;

public record RespostaInsumoNecessarioAcompanhamento(
    String nome,
    int quantidade,
    BigDecimal precoVenda,
    BigDecimal subtotal
) {
    public static RespostaInsumoNecessarioAcompanhamento de(final InsumoNecessario insumo) {
        return new RespostaInsumoNecessarioAcompanhamento(
            insumo.nome().texto().valor(),
            insumo.quantidade().valor(),
            insumo.precoVenda().preco().valor(),
            insumo.subtotal()
        );
    }
}
