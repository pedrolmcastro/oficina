package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioVeiculos {
    private final EntityManager entityManager;
    private final ProtecaoPlaca protecao;

    public RepositorioVeiculos(final EntityManager entityManager, final ProtecaoPlaca protecao) {
        this.entityManager = entityManager;
        this.protecao = protecao;
    }

    public void adicionar(final Veiculo veiculo) {
        this.entityManager.persist(veiculo);
    }

    public Optional<Veiculo> buscar(final UUID id, final LockModeType modoBloqueio) {
        return Optional.ofNullable(this.entityManager.find(Veiculo.class, id, modoBloqueio));
    }

    public Optional<Veiculo> buscar(final Placa placa) {
        final var veiculo = this.entityManager
            .createQuery("FROM Veiculo c WHERE c.placa.hash = :hash", Veiculo.class)
            .setParameter("hash", this.protecao.calcularHash(placa))
            .getSingleResultOrNull();

        return Optional.ofNullable(veiculo);
    }
}
