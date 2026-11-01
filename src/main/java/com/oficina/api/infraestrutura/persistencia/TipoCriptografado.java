package com.oficina.api.infraestrutura.persistencia;

import jakarta.persistence.Column;
import java.io.Serializable;
import java.security.MessageDigest;
import org.hibernate.metamodel.spi.ValueAccess;
import org.jspecify.annotations.Nullable;

/**
 * Conversão Hibernate compartilhada para valores criptografados e autenticados
 * por HMAC.
 */
public abstract class TipoCriptografado<T> extends TipoImutavel<T> {
    private final Criptografia<T> criptografia;
    private final Class<T> tipo;

    protected TipoCriptografado(final Criptografia<T> criptografia, final Class<T> tipo) {
        this.criptografia = criptografia;
        this.tipo = tipo;
    }

    @Override
    public Object getPropertyValue(final T valor, final int propriedade) {
        return switch (propriedade) {
            case 0 -> this.criptografia.criptografar(valor);
            case 1 -> this.criptografia.calcularHash(valor);
            default -> throw new IllegalArgumentException("Propriedade de valor protegido desconhecida");
        };
    }

    @Override
    public @Nullable T instantiate(final ValueAccess valores) {
        final var cifrado = valores.getValue(0, byte[].class);
        if (cifrado == null) {
            return null;
        }
        final var hash = valores.getValue(1, byte[].class);
        if (hash == null) {
            throw new IllegalStateException("Valor persistido sem HMAC");
        }
        return ler(new Colunas(cifrado, hash));
    }

    @Override
    public Class<Colunas> embeddable() {
        return Colunas.class;
    }

    @Override
    public Class<T> returnedClass() {
        return this.tipo;
    }

    @Override
    public @Nullable Serializable disassemble(final @Nullable T valor) {
        return valor == null
            ? null
            : new Colunas(this.criptografia.criptografar(valor), this.criptografia.calcularHash(valor));
    }

    @Override
    public @Nullable T assemble(final @Nullable Serializable cache, final @Nullable Object dono) {
        return switch (cache) {
            case null -> null;
            case Colunas colunas -> ler(colunas);
            default -> throw new IllegalArgumentException("Representação de valor protegido no cache inválida");
        };
    }

    private T ler(final Colunas colunas) {
        final var valor = this.criptografia.descriptografar(colunas.cifrado());
        if (!MessageDigest.isEqual(this.criptografia.calcularHash(valor), colunas.hash())) {
            throw new IllegalStateException("HMAC incompatível com o valor persistido");
        }
        return valor;
    }

    public static final class Colunas implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(nullable = false)
        private final byte[] cifrado;

        @Column(nullable = false)
        private final byte[] hash;

        public Colunas(final byte[] cifrado, final byte[] hash) {
            this.cifrado = cifrado.clone();
            this.hash = hash.clone();
        }

        public byte[] cifrado() {
            return this.cifrado.clone();
        }

        public byte[] hash() {
            return this.hash.clone();
        }
    }
}
