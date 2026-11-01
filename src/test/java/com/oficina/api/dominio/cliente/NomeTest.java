package com.oficina.api.dominio.cliente;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NomeTest {
    @ParameterizedTest
    @ValueSource(strings = { "Nome", "nome", " Nome" })
    void deveAceitarNomeValido() {
        final var valor = " Maria d'Ávila-Silva 123 ";
        final var nome = new Nome(valor);
        assertThat(nome.valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t", "\n", " \t\n " })
    void deveRejeitarNomeEmBranco(final String valor) {
        assertThatThrownBy(() -> new Nome(valor))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Nome não pode estar em branco");
    }
}
