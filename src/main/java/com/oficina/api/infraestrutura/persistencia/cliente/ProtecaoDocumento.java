package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ProtecaoDocumento {
    private static final byte VERSAO = 1;
    private static final int TAMANHO_NONCE = 12;
    private static final int TAMANHO_TAG = 128;
    private final SecretKeySpec chaveCriptografia;
    private final SecretKeySpec chaveHash;
    private final SecureRandom random = new SecureRandom();

    public ProtecaoDocumento(
        @Value(value = "${app.clientes.chave-criptografia}") final String chaveCriptografia,
        @Value(value = "${app.clientes.chave-hash}") final String chaveHash
    ) {
        final var criptografia = decodificarChave(chaveCriptografia);
        final var hash = decodificarChave(chaveHash);
        if (MessageDigest.isEqual(criptografia, hash)) {
            throw new IllegalArgumentException("As chaves de criptografia e HMAC devem ser diferentes");
        }
        this.chaveCriptografia = new SecretKeySpec(criptografia, "AES");
        this.chaveHash = new SecretKeySpec(hash, "HmacSHA256");
    }

    public byte[] criptografar(final Documento documento) {
        final var nonce = new byte[TAMANHO_NONCE];
        this.random.nextBytes(nonce);
        try {
            final var cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
            final var cifrado = cipher.doFinal(serializar(documento));
            return ByteBuffer.allocate(1 + nonce.length + cifrado.length).put(VERSAO).put(nonce).put(cifrado).array();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível criptografar o documento", e);
        }
    }

    public Documento descriptografar(final byte[] envelope) {
        if (envelope.length <= 1 + TAMANHO_NONCE + TAMANHO_TAG / 8 || envelope[0] != VERSAO) {
            throw new IllegalArgumentException("Formato de documento criptografado inválido");
        }
        final var buffer = ByteBuffer.wrap(envelope);
        buffer.get();
        final var nonce = new byte[TAMANHO_NONCE];
        buffer.get(nonce);
        final var cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);
        try {
            final var texto = new String(cipher(Cipher.DECRYPT_MODE, nonce).doFinal(cifrado), StandardCharsets.UTF_8);
            if (texto.startsWith("CPF:")) {
                return new CPF(texto.substring(4));
            }
            if (texto.startsWith("CNPJ:")) {
                return new CNPJ(texto.substring(5));
            }
            throw new IllegalArgumentException("Tipo de documento criptografado inválido");
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível autenticar o documento criptografado", e);
        }
    }

    public byte[] calcularHash(final Documento documento) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(this.chaveHash);
            return mac.doFinal(serializar(documento));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o HMAC do documento", e);
        }
    }

    private Cipher cipher(final int modo, final byte[] nonce) throws GeneralSecurityException {
        final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(modo, this.chaveCriptografia, new GCMParameterSpec(TAMANHO_TAG, nonce));
        cipher.updateAAD(new byte[] { VERSAO, });
        return cipher;
    }

    private static byte[] serializar(final Documento documento) {
        return (documento.tipo().name() + ":" + documento.valor()).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] decodificarChave(final String valor) {
        final var chave = Base64.getDecoder().decode(valor);
        if (chave.length != 32) {
            throw new IllegalArgumentException("As chaves de documentos devem conter 32 bytes em Base64");
        }
        return chave;
    }
}
