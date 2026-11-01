package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.infraestrutura.persistencia.cliente.CriptografiaDocumento;
import com.oficina.api.infraestrutura.persistencia.veiculo.CriptografiaPlaca;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class CriptografiaTest {
    private static final String CHAVE_AES = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String CHAVE_HMAC = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final Criptografia<String> criptografia = texto(CHAVE_AES, CHAVE_HMAC);

    @Test
    void deveCalcularHashDeterministico() {
        final var valor = "dado sensível";
        assertThat(this.criptografia.calcularHash(valor))
            .hasSize(32)
            .isEqualTo(this.criptografia.calcularHash(valor))
            .isNotEqualTo(this.criptografia.calcularHash("outro dado"))
            .isNotEqualTo(texto(/* chaveAes= */ CHAVE_HMAC, /* chaveHmac= */ CHAVE_AES).calcularHash(valor));
    }

    @Test
    void deveCriptografarComNonceAleatorio() {
        final var original = "dado sensível";
        final var primeiro = this.criptografia.criptografar(original);
        assertThat(primeiro).isNotEqualTo(this.criptografia.criptografar(original));
        assertThat(this.criptografia.descriptografar(primeiro)).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource(value = "valores")
    <T> void devePreservarRepresentacaoTextual(final Criptografia<T> criptografia, final T valor, final String texto) {
        assertThat(this.criptografia.descriptografar(criptografia.criptografar(valor))).isEqualTo(texto);
        assertThat(criptografia.descriptografar(this.criptografia.criptografar(texto))).isEqualTo(valor);
        assertThat(criptografia.calcularHash(valor)).isEqualTo(this.criptografia.calcularHash(texto));
    }

    @Test
    void deveRejeitarAdulteracaoOuChaveIncorreta() {
        final var cifrado = this.criptografia.criptografar("dado sensível");
        final var outraCriptografia = texto(/* chaveAes= */ CHAVE_HMAC, /* chaveHmac= */ CHAVE_AES);
        assertThatThrownBy(() -> outraCriptografia.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Não foi possível autenticar o dado criptografado");
        cifrado[cifrado.length - 1] ^= 1;
        assertThatThrownBy(() -> this.criptografia.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Não foi possível autenticar o dado criptografado");
    }

    @Test
    void deveRejeitarChavesIguais() {
        assertThatThrownBy(() -> texto(CHAVE_AES, CHAVE_AES))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("As chaves de criptografia e HMAC devem ser diferentes");
    }

    @ParameterizedTest
    @ValueSource(strings = { "invalido!", "YQ==" })
    void deveRejeitarChavesInvalidas(final String chave) {
        assertThatThrownBy(() -> texto(chave, CHAVE_HMAC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> texto(CHAVE_AES, chave)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarEnvelopeInvalido() {
        assertThatThrownBy(() -> this.criptografia.descriptografar(new byte[0]))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de dado criptografado inválido");
        final var cifrado = this.criptografia.criptografar("dado sensível");
        cifrado[0] = 2;
        assertThatThrownBy(() -> this.criptografia.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de dado criptografado inválido");
    }

    @Test
    void deveRejeitarTipoDeDocumentoInvalido() {
        // Envelope v1 autenticado com CHAVE_AES, nonce zero e conteúdo "RG:123456789".
        final var envelope = Base64.getDecoder().decode("AQAAAAAAAAAAAAAAAJw0mzAgMGtt2Rq5sdkAdCfesV7EGRFTHj/hP+U=");

        assertThatThrownBy(() -> new CriptografiaDocumento(CHAVE_AES, CHAVE_HMAC).descriptografar(envelope))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tipo de documento criptografado inválido");
    }

    @Test
    void devePropagarFalhaAoCriptografar() {
        final var falha = new NoSuchAlgorithmException("AES indisponível");
        try (final var cipher = mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("AES/GCM/NoPadding")).thenThrow(falha);

            final var valor = "dado sensível";

            assertThatThrownBy(() -> this.criptografia.criptografar(valor))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível criptografar o dado")
                .hasCause(falha);
        }
    }

    @Test
    void devePropagarFalhaAoCalcularHash() {
        final var falha = new NoSuchAlgorithmException("HMAC indisponível");
        try (final var mac = mockStatic(Mac.class)) {
            mac.when(() -> Mac.getInstance("HmacSHA256")).thenThrow(falha);

            final var valor = "dado sensível";

            assertThatThrownBy(() -> this.criptografia.calcularHash(valor))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível calcular o HMAC do dado")
                .hasCause(falha);
        }
    }

    private static Stream<Arguments> valores() {
        final var documento = new CriptografiaDocumento(CHAVE_AES, CHAVE_HMAC);
        final var placa = new CriptografiaPlaca(CHAVE_AES, CHAVE_HMAC);
        return Stream
            .of(
                Arguments.of(documento, new CPF("52998224725"), "CPF:52998224725"),
                Arguments.of(documento, new CNPJ("11222333000181"), "CNPJ:11222333000181"),
                Arguments.of(placa, new Placa("ABC1234"), "ABC1234"),
                Arguments.of(placa, new Placa("ABC1D23"), "ABC1D23")
            );
    }

    private static Criptografia<String> texto(final String chaveAes, final String chaveHmac) {
        return new Criptografia<>(chaveAes, chaveHmac) {
            @Override
            protected String serializar(final String valor) {
                return valor;
            }

            @Override
            protected String desserializar(final String texto) {
                return texto;
            }
        };
    }
}
