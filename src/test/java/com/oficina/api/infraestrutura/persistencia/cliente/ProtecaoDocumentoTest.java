package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
            .hasMessage("Não foi possível autenticar o documento criptografado");
        cifrado[cifrado.length - 1] ^= 1;
        assertThatThrownBy(() -> this.protecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Não foi possível autenticar o documento criptografado");
    }

    @Test
    void deveRejeitarChavesIguais() {
        assertThatThrownBy(() -> new ProtecaoDocumento(CHAVE_AES, CHAVE_AES))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("As chaves de criptografia e HMAC devem ser diferentes");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "invalido!, Illegal base64 character 21",
        "YQ==, As chaves de documentos devem conter 32 bytes em Base64" })
    void deveRejeitarChavesInvalidas(final String chave, final String mensagem) {
        assertThatThrownBy(() -> new ProtecaoDocumento(chave, CHAVE_HMAC))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(mensagem);
        assertThatThrownBy(() -> new ProtecaoDocumento(CHAVE_AES, chave))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(mensagem);
    }

    @Test
    void deveRejeitarEnvelopeInvalido() {
        assertThatThrownBy(() -> this.protecao.descriptografar(new byte[0]))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de documento criptografado inválido");
        final var cifrado = this.protecao.criptografar(new CPF("52998224725"));
        cifrado[0] = 2;
        assertThatThrownBy(() -> this.protecao.descriptografar(cifrado))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formato de documento criptografado inválido");
    }

    @Test
    void deveRejeitarTipoDeDocumentoInvalido() throws GeneralSecurityException {
        final var nonce = new byte[12];
        final var cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher
            .init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(CHAVE_AES), "AES"),
                new GCMParameterSpec(128, nonce)
            );
        cipher.updateAAD(new byte[] { 1, });

        final var cifrado = cipher.doFinal("RG:123456789".getBytes(StandardCharsets.UTF_8));

        final var envelope = ByteBuffer
            .allocate(1 + nonce.length + cifrado.length)
            .put((byte) 1)
            .put(nonce)
            .put(cifrado)
            .array();

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
                .hasMessage("Não foi possível criptografar o documento")
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
                .hasMessage("Não foi possível calcular o HMAC do documento")
                .hasCause(falha);
        }
    }
}
