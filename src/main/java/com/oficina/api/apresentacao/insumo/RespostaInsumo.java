package com.oficina.api.apresentacao.insumo;

import com.oficina.api.dominio.insumo.Insumo;
import java.math.BigDecimal;
import java.util.UUID;

public record RespostaInsumo(
    UUID id,
    String nome,
    BigDecimal precoVenda,
    int quantidadeEmEstoque,
    int quantidadeReservada,
    int quantidadeDisponivel
) {
    public static RespostaInsumo de(final Insumo insumo) {
        return new RespostaInsumo(
            insumo.id(),
            insumo.nome().texto().valor(),
            insumo.precoVenda().preco().valor(),
            insumo.quantidadeEmEstoque(),
            insumo.quantidadeReservada(),
            insumo.quantidadeDisponivel()
        );
    }
}
