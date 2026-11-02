package com.oficina.api.infraestrutura.seguranca;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class TokenAcompanhamentoTest {
    @Test
    void deveGerarSegredosIndependentesComHashSha256() {
        final var token = TokenAcompanhamento.gerar();
        assertThat(token).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(TokenAcompanhamento.gerar());
        assertThat(TokenAcompanhamento.hash("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void deveFalharSeProvedorCriptograficoEstiverIndisponivel() {
        try (final var digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException());
            assertThatThrownBy(() -> TokenAcompanhamento.hash("abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }
}
