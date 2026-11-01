package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import java.time.Year;
import java.time.ZoneId;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnoTest {
    @ParameterizedTest
    @MethodSource(value = "anosValidos")
    void deveAceitarAnoValido(final int valor) {
        assertThat(new Ano(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @MethodSource(value = "anosInvalidos")
    void deveRejeitarAnoInvalido(final int valor) {
        assertThatThrownBy(() -> new Ano(valor))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Ano deve estar entre 1886 e " + (Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1));
    }

    private static IntStream anosValidos() {
        final var atual = Year.now(ZoneId.of("America/Sao_Paulo")).getValue();
        return IntStream.of(1886, atual, atual + 1);
    }

    private static IntStream anosInvalidos() {
        return IntStream.of(-1, 0, 1885, Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 2, 9999);
    }
}
