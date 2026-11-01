package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.dominio.Entidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public abstract class RepositorioEntidades<T extends Entidade> {
    protected final EntityManager entityManager;
    private final Class<T> tipo;

    protected RepositorioEntidades(final EntityManager entityManager, final Class<T> tipo) {
        this.entityManager = entityManager;
        this.tipo = tipo;
    }

    public void adicionar(final T entidade) {
        this.entityManager.persist(entidade);
    }

    public Optional<T> buscar(final UUID id, final LockModeType modoBloqueio) {
        return Optional.ofNullable(this.entityManager.find(this.tipo, id, modoBloqueio));
    }
}
