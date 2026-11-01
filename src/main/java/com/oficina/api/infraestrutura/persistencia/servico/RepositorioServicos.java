package com.oficina.api.infraestrutura.persistencia.servico;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.servico.Servico;
import com.oficina.api.infraestrutura.persistencia.BuscaTextual;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioServicos extends RepositorioEntidades<Servico> {
    private final BuscaTextual<Servico> buscaTextual;

    public RepositorioServicos(final EntityManager entityManager) {
        super(entityManager, Servico.class);

        this.buscaTextual = new BuscaTextual<>(
            entityManager,
            Servico.class,
            "SELECT * FROM oficina.servicos WHERE deletado = FALSE",
            "id",
            List.of("nome")
        );
    }

    public List<Servico> listar(final @Nullable String nome, final @Nullable UUID after, final Pagina.Limite limite) {
        return this.buscaTextual.fulltextSearch(nome, after, limite);
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

    public List<Insumo> listarInsumosObrigatorios(
        final UUID servicoId,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
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
        return consulta.setMaxResults(limite.quantidadeParaConsulta()).getResultList();
    }
}
