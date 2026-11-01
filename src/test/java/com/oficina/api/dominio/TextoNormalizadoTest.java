package com.oficina.api.dominio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextoNormalizadoTest {
    @ParameterizedTest
    @ValueSource(strings = { "Nome", "nome", "Maria d'Ávila-Silva 123", "Maria  Silva", "Óleo", "A\tB" })
    void devePreservarTextoValido(final String valor) {
        assertThat(new TextoNormalizado(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t", "\n", " \t\n ", " " }) // O último caso é um espaço EM (U+2003).
    void deveRejeitarTextoEmBranco(final String valor) {
        assertThatThrownBy(() -> new TextoNormalizado(valor))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Texto não pode estar em branco");
    }

    @ParameterizedTest
    @ValueSource(strings = { " Nome", "Nome ", " Nome ", "\tNome", "Nome\n" })
    void deveRejeitarEspacosNasExtremidades(final String valor) {
        assertThatThrownBy(() -> new TextoNormalizado(valor))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Texto não pode conter espaços no início ou no fim");
    }
}
