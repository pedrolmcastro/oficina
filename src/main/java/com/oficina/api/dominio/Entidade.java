package com.oficina.api.dominio;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@MappedSuperclass
public abstract class Entidade {
    @Id
    private UUID id = UUID.ofEpochMillis(System.currentTimeMillis());

    protected Entidade() {}

    public UUID id() {
        return this.id;
    }

    @Override
    public boolean equals(final @Nullable Object outro) {
        return outro instanceof Entidade entidade && id().equals(entidade.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
}
