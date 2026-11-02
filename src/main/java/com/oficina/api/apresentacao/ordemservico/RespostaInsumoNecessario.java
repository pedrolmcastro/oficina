package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.dominio.ordemservico.InsumoNecessario;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaInsumoNecessario(
    UUID id,
    UUID insumoId,
    @Nullable
    UUID servicoContratadoId,
    String nome,
    int quantidade,
    BigDecimal precoUnitario,
    BigDecimal subtotal
) {
    public static RespostaInsumoNecessario de(final InsumoNecessario item) {
        return new RespostaInsumoNecessario(
            item.id(),
            item.insumoId(),
            item.servicoContratadoId(),
            item.nome().texto().valor(),
            item.quantidade().valor(),
            item.precoUnitario().preco().valor(),
            item.subtotal()
        );
    }
}
