package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Preco;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;

@Embeddable
public record PrecoVenda(
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "preco_venda", nullable = false, precision = 19,
        scale = 2))
    Preco preco
) {
    private static final Preco VALOR_MAXIMO = new Preco(new BigDecimal("10000.00"));

    public PrecoVenda {
        if (preco.compareTo(VALOR_MAXIMO) > 0) {
            throw new DadoInvalidoException("Preço de venda deve estar entre 0 e 10000.00");
        }
    }
}
