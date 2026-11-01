package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacaTest {
    @ParameterizedTest
    @ValueSource(strings = { "ABC1234", "ABC1D23", "ZZZ9Z99", "AAA0000" })
    void deveAceitarFormatosBrasileiros(final String valor) {
        assertThat(new Placa(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        " ",
        "abc1234",
        "ABC-1234",
        "ABC-1D23",
        "ABC- 1234",
        "AB12345",
        "ABCD123",
        "ABC123",
        "ABC12345",
        "ABC12D3",
        "ABC 1234",
        " ABC1234",
        "ABC1234\n",
        "ÁBC1234",
        "ABC١٢٣٤",
        "ABC1D2X" })
    void deveRejeitarFormatoInvalido(final String valor) {
        assertThatThrownBy(() -> new Placa(valor))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Placa deve seguir o formato ABC1234 ou ABC1D23, sem hífen");
    }

    @Test
    void deveOcultarValor() {
        final var valor = "ABC1234";
        assertThat(new Placa(valor).toString()).doesNotContain(valor);
    }
}
