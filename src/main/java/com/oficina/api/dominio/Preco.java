package com.oficina.api.dominio;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Preco(BigDecimal valor) implements Comparable<Preco> {
    public Preco {
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException _) {
            throw new DadoInvalidoException("Preço deve ter no máximo duas casas decimais");
        }
        if (valor.signum() < 0) {
            throw new DadoInvalidoException("Preço não pode ser negativo");
        }
    }

    @Override
    public int compareTo(final Preco outro) {
        return this.valor.compareTo(outro.valor);
    }
}
