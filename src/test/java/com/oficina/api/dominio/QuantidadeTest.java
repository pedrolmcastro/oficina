package com.oficina.api.dominio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantidadeTest {
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 100, Integer.MAX_VALUE })
    void devePreservarQuantidadeInteira(final int valor) {
        assertThat(new Quantidade(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
    void deveRejeitarQuantidadeNaoPositiva(final int valor) {
        assertThatThrownBy(() -> new Quantidade(valor))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Quantidade deve ser um inteiro positivo");
    }
}
