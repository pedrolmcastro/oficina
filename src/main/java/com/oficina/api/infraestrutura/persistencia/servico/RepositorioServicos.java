package com.oficina.api.infraestrutura.persistencia.servico;

import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.servico.Servico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioServicos {
    private final EntityManager entityManager;

    public RepositorioServicos(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void adicionar(final Servico servico) {
        this.entityManager.persist(servico);
    }

    public Optional<Servico> buscar(final UUID id, final LockModeType modoBloqueio) {
        return Optional.ofNullable(this.entityManager.find(Servico.class, id, modoBloqueio));
    }

    public List<Servico> listar(final @Nullable String nome, final @Nullable UUID after, final int first) {
        final var sql = new StringBuilder("SELECT * FROM oficina.servicos WHERE deletado = FALSE");
        if (nome != null) {
            sql.append(" AND to_tsvector('portuguese', nome) @@ plainto_tsquery('portuguese', :nome)");
        }
        if (after != null) {
            sql.append(" AND id > :after");
        }
        sql.append(" ORDER BY id ASC");
        final var consulta = this.entityManager.unwrap(Session.class).createNativeQuery(sql.toString(), Servico.class);
        if (nome != null) {
            consulta.setParameter("nome", nome);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(first).getResultList();
    }

    public void associarInsumoObrigatorio(final UUID servicoId, final UUID insumoId) {
        this.entityManager
            .createNativeQuery(
                "INSERT INTO oficina.servicos_insumos_obrigatorios (servico_id, insumo_id) VALUES (:servico, :insumo)",
                Void.class
            )
            .setParameter("servico", servicoId)
            .setParameter("insumo", insumoId)
            .executeUpdate();
    }

    public boolean removerInsumoObrigatorio(final UUID servicoId, final UUID insumoId) {
        return this.entityManager
            .createNativeQuery(
                "DELETE FROM oficina.servicos_insumos_obrigatorios WHERE servico_id = :servico AND insumo_id = :insumo",
                Void.class
            )
            .setParameter("servico", servicoId)
            .setParameter("insumo", insumoId)
            .executeUpdate() > 0;
    }

    public List<Insumo> listarInsumosObrigatorios(final UUID servicoId, final @Nullable UUID after, final int first) {
        final var sql = new StringBuilder(
            "SELECT i.* FROM oficina.insumos i "
                + "JOIN oficina.servicos_insumos_obrigatorios si ON si.insumo_id = i.id "
                + "WHERE si.servico_id = :servico AND i.deletado = FALSE"
        );
        if (after != null) {
            sql.append(" AND i.id > :after");
        }
        sql.append(" ORDER BY i.id ASC");
        final var consulta = this.entityManager.unwrap(Session.class).createNativeQuery(sql.toString(), Insumo.class);
        consulta.setParameter("servico", servicoId);
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(first).getResultList();
    }
}
