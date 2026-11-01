package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import jakarta.persistence.Column;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Objects;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public final class PlacaPersistida implements CompositeUserType<Placa> {
    private final ProtecaoPlaca protecao;

    public PlacaPersistida(final ProtecaoPlaca protecao) {
        this.protecao = protecao;
    }

    @Override
    public Object getPropertyValue(final Placa placa, final int propriedade) {
        return switch (propriedade) {
            case 0 -> this.protecao.criptografar(placa);
            case 1 -> this.protecao.calcularHash(placa);
            default -> throw new IllegalArgumentException("Propriedade de placa desconhecida");
        };
    }

    @Override
    public @Nullable Placa instantiate(final ValueAccess valores) {
        final var cifrado = valores.getValue(0, byte[].class);
        if (cifrado == null) {
            return null;
        }
        final var hash = valores.getValue(1, byte[].class);
        if (hash == null) {
            throw new IllegalStateException("Placa persistida sem HMAC");
        }
        return ler(new Colunas(cifrado, hash));
    }

    @Override
    public Class<Colunas> embeddable() {
        return Colunas.class;
    }

    @Override
    public Class<Placa> returnedClass() {
        return Placa.class;
    }

    @Override
    public boolean equals(final @Nullable Placa primeiro, final @Nullable Placa segundo) {
        return Objects.equals(primeiro, segundo);
    }

    @Override
    public int hashCode(final @Nullable Placa placa) {
        return Objects.hashCode(placa);
    }

    @Override
    public @Nullable Placa deepCopy(final @Nullable Placa placa) {
        return placa;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public @Nullable Serializable disassemble(final @Nullable Placa placa) {
        return placa == null
            ? null
            : new Colunas(this.protecao.criptografar(placa), this.protecao.calcularHash(placa));
    }

    @Override
    public @Nullable Placa assemble(final @Nullable Serializable cache, final @Nullable Object dono) {
        return switch (cache) {
            case null -> null;
            case Colunas colunas -> ler(colunas);
            default -> throw new IllegalArgumentException("Representação de placa no cache inválida");
        };
    }

    @Override
    public @Nullable Placa replace(
        final @Nullable Placa original,
        final @Nullable Placa atual,
        final @Nullable Object dono
    ) {
        return original;
    }

    private Placa ler(final Colunas colunas) {
        final var placa = this.protecao.descriptografar(colunas.cifrado());
        if (!MessageDigest.isEqual(this.protecao.calcularHash(placa), colunas.hash())) {
            throw new IllegalStateException("HMAC incompatível com a placa persistida");
        }
        return placa;
    }

    public static final class Colunas implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "placa_criptografada", nullable = false)
        private final byte[] cifrado;

        @Column(name = "hash_placa", nullable = false)
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
