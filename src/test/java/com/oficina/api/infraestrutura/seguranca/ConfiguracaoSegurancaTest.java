package com.oficina.api.infraestrutura.seguranca;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.oficina.api.infraestrutura.seguranca.ConfiguracaoSeguranca.TAMANHO_MINIMO_SEGREDO_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ConfiguracaoSegurancaTest {
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, TAMANHO_MINIMO_SEGREDO_BYTES / 2, TAMANHO_MINIMO_SEGREDO_BYTES - 1 })
    void deveRejeitarSegredoCurto(final int tamanho) {
        final var propriedades = propriedades(new byte[tamanho]);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ConfiguracaoSeguranca().secretKey(propriedades))
            .withMessageContaining("pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes");
    }

    @Test
    void deveAceitarSegredoComTamanhoMinimo() {
        final var bytes = new byte[TAMANHO_MINIMO_SEGREDO_BYTES];
        final var chave = new ConfiguracaoSeguranca().secretKey(propriedades(bytes));

        assertThat(chave.getEncoded()).containsExactly(bytes);
        assertThat(chave.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    private static PropriedadesSeguranca propriedades(final byte[] bytes) {
        final var segredo = Base64.getEncoder().encodeToString(bytes);
        return new PropriedadesSeguranca(
            new PropriedadesSeguranca.Admin("admin", "hash"),
            new PropriedadesSeguranca.Jwt(segredo, Duration.ofMinutes(30))
        );
    }
}
