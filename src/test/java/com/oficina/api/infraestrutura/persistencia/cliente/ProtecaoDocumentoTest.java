package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class ProtecaoDocumentoTest {
    private static final String CHAVE_AES = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String CHAVE_HMAC = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final ProtecaoDocumento protecao = new ProtecaoDocumento(CHAVE_AES, CHAVE_HMAC);

    @Test
    void deveCalcularHashDeterministico() {
        final var cpf = new CPF("52998224725");
        assertThat(this.protecao.calcularHash(cpf))
            .hasSize(32)
            .isEqualTo(this.protecao.calcularHash(cpf))
            .isNotEqualTo(this.protecao.calcularHash(new CPF("11144477735")))
            .isNotEqualTo(new ProtecaoDocumento(CHAVE_HMAC, CHAVE_AES).calcularHash(cpf));
    }

    @Test
    void deveCriptografarDocumento() {
        final var cpf = new CPF("52998224725");
        final var primeiro = this.protecao.criptografar(cpf);
        final var segundo = this.protecao.criptografar(cpf);
        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(this.protecao.descriptografar(primeiro)).isEqualTo(cpf);
        final var cnpj = new CNPJ("11222333000181");
        assertThat(this.protecao.descriptografar(this.protecao.criptografar(cnpj))).isEqualTo(cnpj);
    }

    @Test
    void deveRejeitarAdulteracaoOuChaveIncorreta() {
        final var cifrado = this.protecao.criptografar(new CPF("52998224725"));
        final var outraProtecao = new ProtecaoDocumento(CHAVE_HMAC, CHAVE_AES);
        assertThatThrownBy(() -> outraProtecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Não foi possível autenticar o dado criptografado");
        cifrado[cifrado.length - 1] ^= 1;
        assertThatThrownBy(() -> this.protecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Não foi possível autenticar o dado criptografado");
    }

    @Test
    void deveRejeitarChavesIguais() {
        assertThatThrownBy(() -> new ProtecaoDocumento(CHAVE_AES, CHAVE_AES))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("As chaves de criptografia e HMAC devem ser diferentes");
    }

    @ParameterizedTest
    @ValueSource(strings = { "invalido!", "YQ==" })
    void deveRejeitarChavesInvalidas(final String chave) {
        assertThatThrownBy(() -> new ProtecaoDocumento(chave, CHAVE_HMAC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProtecaoDocumento(CHAVE_AES, chave)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarEnvelopeInvalido() {
        assertThatThrownBy(() -> this.protecao.descriptografar(new byte[0]))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de dado criptografado inválido");
        final var cifrado = this.protecao.criptografar(new CPF("52998224725"));
        cifrado[0] = 2;
        assertThatThrownBy(() -> this.protecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de dado criptografado inválido");
    }

    @Test
    void deveRejeitarTipoDeDocumentoInvalido() {
        // Envelope v1 autenticado com CHAVE_AES, nonce zero e conteúdo "RG:123456789".
        final var envelope = Base64.getDecoder().decode("AQAAAAAAAAAAAAAAAJw0mzAgMGtt2Rq5sdkAdCfesV7EGRFTHj/hP+U=");

        assertThatThrownBy(() -> this.protecao.descriptografar(envelope))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tipo de documento criptografado inválido");
    }

    @Test
    void devePropagarFalhaAoCriptografar() {
        final var falha = new NoSuchAlgorithmException("AES indisponível");
        try (final var cipher = mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("AES/GCM/NoPadding")).thenThrow(falha);

            final var cpf = new CPF("52998224725");

            assertThatThrownBy(() -> this.protecao.criptografar(cpf))
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

            final var cpf = new CPF("52998224725");

            assertThatThrownBy(() -> this.protecao.calcularHash(cpf))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível calcular o HMAC do dado")
                .hasCause(falha);
        }
    }
}
