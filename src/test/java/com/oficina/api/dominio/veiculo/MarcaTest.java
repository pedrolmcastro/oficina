package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarcaTest {
    @ParameterizedTest
    @ValueSource(strings = { "Toyota", "toyota", " Toyota", " Mercedes-Benz " })
    void deveAceitarMarcaValida(final String valor) {
        final var marca = new Marca(valor);
        assertThat(marca.valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t", "\n", " \t\n " })
    void deveRejeitarMarcaEmBranco(final String valor) {
        assertThatThrownBy(() -> new Marca(valor))
            .isInstanceOf(DadoInvalidoException.class)
            .hasMessage("Marca não pode estar em branco");
    }
}
