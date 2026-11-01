package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.DadoInvalidoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {
    @ParameterizedTest
    @ValueSource(strings = { "nome@example.com", "nome.sobrenome+oficina@example.com.br", "contato@oficina.com" })
    void deveAceitarEmailValido(final String valor) {
        final var email = new Email(valor);
        assertThat(email.valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        " ",
        "\t",
        "maria",
        "@example.com",
        "maria@",
        "maria@@example.com",
        "maria..silva@example.com",
        "maria@example..com",
        "maria silva@example.com" })
    void deveRejeitarEmailInvalido(final String valor) {
        assertThatThrownBy(() -> new Email(valor))
            .isInstanceOf(DadoInvalidoException.class)
            .hasMessage("Email inválido");
    }
}
