package com.oficina.api.infraestrutura.persistencia;

import java.util.Objects;
import org.hibernate.usertype.CompositeUserType;
import org.jspecify.annotations.Nullable;

public abstract class TipoImutavel<T> implements CompositeUserType<T> {
    @Override
    public boolean equals(final @Nullable T primeiro, final @Nullable T segundo) {
        return Objects.equals(primeiro, segundo);
    }

    @Override
    public int hashCode(final @Nullable T valor) {
        return Objects.hashCode(valor);
    }

    @Override
    public @Nullable T deepCopy(final @Nullable T valor) {
        return valor;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public @Nullable T replace(final @Nullable T original, final @Nullable T atual, final @Nullable Object dono) {
        return original;
    }
}
