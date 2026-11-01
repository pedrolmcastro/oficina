package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record PrecoVenda(
    @Column(name = "preco_venda", nullable = false, precision = 19, scale = 2)
    BigDecimal valor
) {
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("10000.00");

    public PrecoVenda {
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException _) {
            throw new DadoInvalidoException("Preço de venda deve ter no máximo duas casas decimais");
        }
        if (valor.signum() < 0 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new DadoInvalidoException("Preço de venda deve estar entre 0 e 10000.00");
        }
    }
}
