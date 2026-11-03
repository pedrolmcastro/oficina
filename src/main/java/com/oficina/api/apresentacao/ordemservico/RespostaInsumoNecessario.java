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
    int quantidadeReservada,
    int quantidadeConsumida,
    int quantidadePendente,
    BigDecimal precoVenda,
    BigDecimal subtotal
) {
    public static RespostaInsumoNecessario de(final InsumoNecessario insumoNecessario) {
        return new RespostaInsumoNecessario(
            insumoNecessario.id(),
            insumoNecessario.insumoId(),
            insumoNecessario.servicoContratadoId(),
            insumoNecessario.nome().texto().valor(),
            insumoNecessario.quantidade().valor(),
            insumoNecessario.quantidadeReservada(),
            insumoNecessario.quantidadeConsumida(),
            insumoNecessario.quantidadePendente(),
            insumoNecessario.precoVenda().preco().valor(),
            insumoNecessario.subtotal()
        );
    }
}
