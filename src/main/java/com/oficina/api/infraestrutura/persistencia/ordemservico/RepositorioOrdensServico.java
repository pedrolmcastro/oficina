package com.oficina.api.infraestrutura.persistencia.ordemservico;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.ordemservico.OrdemServico;
import com.oficina.api.dominio.ordemservico.StatusOrdemServico;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioOrdensServico extends RepositorioEntidades<OrdemServico> {
    public RepositorioOrdensServico(final EntityManager entityManager) {
        super(entityManager, OrdemServico.class);
    }

    @Override
    public Optional<OrdemServico> buscar(final UUID id, final LockModeType modo) {
        return super.buscar(id, modo).map(RepositorioOrdensServico::carregar);
    }

    public Optional<OrdemServico> buscarPorToken(final String hash, final LockModeType modo) {
        return this.entityManager
            .createQuery(
                "SELECT o FROM OrdemServico o WHERE o.hashToken = :hash AND o.tokenExpiraEm > :agora",
                OrdemServico.class
            )
            .setParameter("hash", hash)
            .setParameter("agora", Instant.now())
            .setLockMode(modo)
            .getResultList()
            .stream()
            .findFirst()
            .map(RepositorioOrdensServico::carregar);
    }

    public Optional<OrdemServico> buscarPorTokenDecisao(final String hash, final LockModeType modo) {
        return this.entityManager
            .createQuery(
                "SELECT o FROM OrdemServico o WHERE o.hashTokenDecisao = :hash AND o.decisaoExpiraEm > :agora",
                OrdemServico.class
            )
            .setParameter("hash", hash)
            .setParameter("agora", Instant.now())
            .setLockMode(modo)
            .getResultList()
            .stream()
            .findFirst()
            .map(RepositorioOrdensServico::carregar);
    }

    public List<OrdemServico> listar(
        final @Nullable StatusOrdemServico status,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
        final var jpql = new StringBuilder("SELECT o FROM OrdemServico o WHERE 1 = 1");
        if (status != null) {
            jpql.append(" AND o.status = :status");
        }
        if (after != null) {
            jpql.append(" AND o.id > :after");
        }
        jpql.append(" ORDER BY o.id ASC");
        final var consulta = this.entityManager.createQuery(jpql.toString(), OrdemServico.class);
        if (status != null) {
            consulta.setParameter("status", status);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta
            .setMaxResults(limite.quantidadeParaConsulta())
            .getResultList()
            .stream()
            .map(RepositorioOrdensServico::carregar)
            .toList();
    }

    public List<OrdemServico> aguardandoInsumo(final UUID insumoId) {
        return this.entityManager
            .createQuery(
                "SELECT o FROM OrdemServico o WHERE o.status = :status AND EXISTS "
                    + "(SELECT i.id FROM InsumoNecessario i WHERE i MEMBER OF o.insumosNecessarios "
                    + "AND i.insumoId = :insumoId "
                    + "AND i.quantidadeReservada + i.quantidadeConsumida < i.quantidade.valor) "
                    + "ORDER BY o.criadaEm ASC, o.id ASC",
                OrdemServico.class
            )
            .setParameter("status", StatusOrdemServico.AGUARDANDO_INSUMOS)
            .setParameter("insumoId", insumoId)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList()
            .stream()
            .map(RepositorioOrdensServico::carregar)
            .toList();
    }

    private static OrdemServico carregar(final OrdemServico ordem) {
        // A API recebe o agregado completo antes do encerramento da transação
        // (open-in-view desabilitado).
        ordem.servicosContratados();
        ordem.insumosNecessarios();
        ordem.eventos();
        return ordem;
    }
}
