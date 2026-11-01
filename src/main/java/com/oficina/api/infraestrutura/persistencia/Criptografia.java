package com.oficina.api.infraestrutura.persistencia;

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

/**
 * Criptografa valores com AES-GCM e calcula HMAC a partir de sua representação
 * textual canônica.
 */
public abstract class Criptografia<T> {
    private static final byte VERSAO = 1;
    private static final int TAMANHO_NONCE = 12;
    private static final int TAMANHO_TAG = 128;
    private final SecretKeySpec chaveCriptografia;
    private final SecretKeySpec chaveHash;
    private final SecureRandom random = new SecureRandom();

    private Criptografia(final Chaves chaves) {
        this.chaveCriptografia = chaves.criptografia();
        this.chaveHash = chaves.hash();
    }

    protected Criptografia(final String chaveCriptografia, final String chaveHash) {
        this(Chaves.de(chaveCriptografia, chaveHash));
    }

    public byte[] criptografar(final T valor) {
        final var nonce = new byte[TAMANHO_NONCE];
        this.random.nextBytes(nonce);
        try {
            final var cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
            final var cifrado = cipher.doFinal(serializar(valor).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + nonce.length + cifrado.length).put(VERSAO).put(nonce).put(cifrado).array();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível criptografar o dado", e);
        }
    }

    public T descriptografar(final byte[] envelope) {
        if (envelope.length <= 1 + TAMANHO_NONCE + TAMANHO_TAG / 8 || envelope[0] != VERSAO) {
            throw new IllegalArgumentException("Formato de dado criptografado inválido");
        }
        final var buffer = ByteBuffer.wrap(envelope);
        buffer.get();
        final var nonce = new byte[TAMANHO_NONCE];
        buffer.get(nonce);
        final var cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);
        try {
            return desserializar(
                new String(cipher(Cipher.DECRYPT_MODE, nonce).doFinal(cifrado), StandardCharsets.UTF_8)
            );
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível autenticar o dado criptografado", e);
        }
    }

    public byte[] calcularHash(final T valor) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(this.chaveHash);
            return mac.doFinal(serializar(valor).getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o HMAC do dado", e);
        }
    }

    protected abstract String serializar(T valor);

    protected abstract T desserializar(String texto);

    private Cipher cipher(final int modo, final byte[] nonce) throws GeneralSecurityException {
        final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(modo, this.chaveCriptografia, new GCMParameterSpec(TAMANHO_TAG, nonce));
        cipher.updateAAD(new byte[] { VERSAO, });
        return cipher;
    }

    private static record Chaves(SecretKeySpec criptografia, SecretKeySpec hash) {
        private static byte[] decodificarChave(final String valor) {
            final var chave = Base64.getDecoder().decode(valor);
            if (chave.length != 32) {
                throw new IllegalArgumentException("As chaves de dados devem conter 32 bytes em Base64");
            }
            return chave;
        }

        private static Chaves de(final String chaveCriptografia, final String chaveHash) {
            final var criptografia = decodificarChave(chaveCriptografia);
            final var hash = decodificarChave(chaveHash);
            if (MessageDigest.isEqual(criptografia, hash)) {
                throw new IllegalArgumentException("As chaves de criptografia e HMAC devem ser diferentes");
            }
            return new Chaves(new SecretKeySpec(criptografia, "AES"), new SecretKeySpec(hash, "HmacSHA256"));
        }
    }
}
