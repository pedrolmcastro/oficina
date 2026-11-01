package com.oficina.api.dominio;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecoTest {
    @ParameterizedTest
    @ValueSource(strings = { "0", "0.01", "19.90", "19.900", "10000.01", "1E+100" })
    void devePreservarValorExatoComDuasCasas(final String valor) {
        final var preco = new Preco(new BigDecimal(valor));
        assertThat(preco.valor()).isEqualByComparingTo(valor);
        assertThat(preco.valor().scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "-0.01", "-10000" })
    void deveRejeitarValorNegativo(final String valor) {
        final var decimal = new BigDecimal(valor);
        assertThatThrownBy(() -> new Preco(decimal))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Preço não pode ser negativo");
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.001", "19.999", "-0.001" })
    void deveRejeitarFracoesDeCentavoSemArredondar(final String valor) {
        final var decimal = new BigDecimal(valor);
        assertThatThrownBy(() -> new Preco(decimal))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Preço deve ter no máximo duas casas decimais");
    }

    @ParameterizedTest
    @CsvSource(value = { "0, 0.01, -1", "19.90, 19.900, 0", "10000, 19.90, 1" })
    void deveCompararPorValor(final String primeiro, final String segundo, final int sinal) {
        final var a = new Preco(new BigDecimal(primeiro));
        final var b = new Preco(new BigDecimal(segundo));
        assertThat(Integer.signum(a.compareTo(b))).isEqualTo(sinal);
    }
}
