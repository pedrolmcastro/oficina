package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento;
import jakarta.persistence.Column;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Objects;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public final class DocumentoPersistido implements CompositeUserType<Documento> {
    private final ProtecaoDocumento protecao;

    public DocumentoPersistido(final ProtecaoDocumento protecao) {
        this.protecao = protecao;
    }

    @Override
    public Object getPropertyValue(final Documento documento, final int propriedade) {
        return switch (propriedade) {
            case 0 -> this.protecao.criptografar(documento);
            case 1 -> this.protecao.calcularHash(documento);
            default -> throw new IllegalArgumentException("Propriedade de documento desconhecida");
        };
    }

    @Override
    public @Nullable Documento instantiate(final ValueAccess valores) {
        final var cifrado = valores.getValue(0, byte[].class);
        if (cifrado == null) {
            return null;
        }
        final var hash = valores.getValue(1, byte[].class);
        if (hash == null) {
            throw new IllegalStateException("Documento persistido sem HMAC");
        }
        return ler(new Colunas(cifrado, hash));
    }

    @Override
    public Class<Colunas> embeddable() {
        return Colunas.class;
    }

    @Override
    public Class<Documento> returnedClass() {
        return Documento.class;
    }

    @Override
    public boolean equals(final @Nullable Documento primeiro, final @Nullable Documento segundo) {
        return Objects.equals(primeiro, segundo);
    }

    @Override
    public int hashCode(final @Nullable Documento documento) {
        return Objects.hashCode(documento);
    }

    @Override
    public @Nullable Documento deepCopy(final @Nullable Documento documento) {
        return documento;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public @Nullable Serializable disassemble(final @Nullable Documento documento) {
        return documento == null
            ? null
            : new Colunas(this.protecao.criptografar(documento), this.protecao.calcularHash(documento));
    }

    @Override
    public @Nullable Documento assemble(final @Nullable Serializable cache, final @Nullable Object dono) {
        return switch (cache) {
            case null -> null;
            case Colunas colunas -> ler(colunas);
            default -> throw new IllegalArgumentException("Representação de documento no cache inválida");
        };
    }

    @Override
    public @Nullable Documento replace(
        final @Nullable Documento original,
        final @Nullable Documento atual,
        final @Nullable Object dono
    ) {
        return original;
    }

    private Documento ler(final Colunas colunas) {
        final var documento = this.protecao.descriptografar(colunas.cifrado());
        if (!MessageDigest.isEqual(this.protecao.calcularHash(documento), colunas.hash())) {
            throw new IllegalStateException("HMAC incompatível com o documento persistido");
        }
        return documento;
    }

    public static final class Colunas implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "documento_criptografado", nullable = false)
        private final byte[] cifrado;

        @Column(name = "hash_documento", nullable = false)
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
