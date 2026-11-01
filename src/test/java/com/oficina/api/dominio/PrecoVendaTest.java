package com.oficina.api.dominio;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecoVendaTest {
    @ParameterizedTest
    @MethodSource(value = "precos")
    void devePreservarPrecoDentroDosLimites(final Function<Preco, Preco> criar, final String valor) {
        assertThat(criar.apply(new Preco(new BigDecimal(valor))).valor()).isEqualByComparingTo(valor);
    }

    @ParameterizedTest
    @MethodSource(value = "precosAcimaDoLimite")
    void deveRejeitarPrecoAcimaDoMaximo(final Function<Preco, Preco> criar, final String valor) {
        final var preco = new Preco(new BigDecimal(valor));
        assertThatThrownBy(() -> criar.apply(preco))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Preço de venda deve estar entre 0 e 10000.00");
    }

    private static Stream<Arguments> precos() {
        return casos("0", "10000.00");
    }

    private static Stream<Arguments> precosAcimaDoLimite() {
        return casos("10000.01");
    }

    private static Stream<Arguments> casos(final String... valores) {
        return Stream
            .<Function<Preco, Preco>>of(
                preco -> new com.oficina.api.dominio.insumo.PrecoVenda(preco).preco(),
                preco -> new com.oficina.api.dominio.servico.PrecoVenda(preco).preco()
            )
            .flatMap(criar -> Stream.of(valores).map(valor -> Arguments.of(criar, valor)));
    }
}
