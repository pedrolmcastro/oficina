package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloTest {
    @ParameterizedTest
    @ValueSource(strings = { "Corolla", "corolla", " Corolla", " Corolla Cross XRE 2.0 " })
    void deveAceitarModeloValido(final String valor) {
        final var modelo = new Modelo(valor);
        assertThat(modelo.valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t", "\n", " \t\n " })
    void deveRejeitarModeloEmBranco(final String valor) {
        assertThatThrownBy(() -> new Modelo(valor))
            .isInstanceOf(DadoInvalidoException.class)
            .hasMessage("Modelo não pode estar em branco");
    }
}
