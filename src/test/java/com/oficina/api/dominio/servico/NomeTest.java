package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.DadoInvalidoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NomeTest {
    @ParameterizedTest
    @ValueSource(strings = { "Óleo de motor", " Óleo de motor Cross XRE 2.0 " })
    void deveAceitarNomeValido(final String valor) {
        final var nome = new Nome(valor);
        assertThat(nome.valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t\n" })
    void deveRejeitarNomeEmBranco(final String valor) {
        assertThatThrownBy(() -> new Nome(valor))
            .isInstanceOf(DadoInvalidoException.class)
            .hasMessage("Nome não pode estar em branco");
    }
}
