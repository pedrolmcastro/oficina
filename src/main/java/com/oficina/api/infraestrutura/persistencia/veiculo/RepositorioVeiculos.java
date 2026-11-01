package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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

    public List<Veiculo> listar(
        final @Nullable Placa placa,
        final @Nullable UUID cliente,
        final @Nullable UUID after,
        final int first
    ) {
        final var hql = new StringBuilder("FROM Veiculo v WHERE 1 = 1");
        if (placa != null) {
            hql.append(" AND v.placa.hash = :hash");
        }
        if (cliente != null) {
            hql.append(" AND v.clienteId = :cliente");
        }
        if (after != null) {
            hql.append(" AND v.id > :after");
        }
        hql.append(" ORDER BY v.id ASC");
        final var consulta = this.entityManager.createQuery(hql.toString(), Veiculo.class);
        if (placa != null) {
            consulta.setParameter("hash", this.protecao.calcularHash(placa));
        }
        if (cliente != null) {
            consulta.setParameter("cliente", cliente);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(first).getResultList();
    }
}
