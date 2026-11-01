package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtecaoPlacaTest {
    private static final String CHAVE_AES = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String CHAVE_HMAC = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final ProtecaoPlaca protecao = new ProtecaoPlaca(CHAVE_AES, CHAVE_HMAC);

    @Test
    void deveCriptografarAmbosFormatosComNonceAleatorio() {
        for (final var placa : new Placa[] { new Placa("ABC1234"), new Placa("ABC1D23"), }) {
            final var cifrado = this.protecao.criptografar(placa);
            assertThat(cifrado).isNotEqualTo(this.protecao.criptografar(placa));
            assertThat(new String(cifrado, StandardCharsets.ISO_8859_1)).doesNotContain(placa.valor());
            assertThat(this.protecao.descriptografar(cifrado)).isEqualTo(placa);
        }
    }

    @Test
    void deveCalcularHashDeterministico() {
        final var hash = this.protecao.calcularHash(new Placa("ABC1234"));
        assertThat(hash)
            .hasSize(32)
            .isEqualTo(this.protecao.calcularHash(new Placa("ABC1234")))
            .isNotEqualTo(this.protecao.calcularHash(new Placa("ABC1D23")))
            .isNotEqualTo(new ProtecaoPlaca(CHAVE_HMAC, CHAVE_AES).calcularHash(new Placa("ABC1234")));
    }

    @Test
    void deveRejeitarAdulteracaoEChaveIncorreta() {
        final var cifrado = this.protecao.criptografar(new Placa("ABC1234"));
        assertThatThrownBy(() -> new ProtecaoPlaca(CHAVE_HMAC, CHAVE_AES).descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class);
        cifrado[cifrado.length - 1] ^= 1;
        assertThatThrownBy(() -> this.protecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class);
    }
}
