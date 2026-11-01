package com.oficina.api.infraestrutura.persistencia.insumo;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.infraestrutura.persistencia.BuscaTextual;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioInsumos extends RepositorioEntidades<Insumo> {
    private final BuscaTextual<Insumo> buscaTextual;

    public RepositorioInsumos(final EntityManager entityManager) {
        super(entityManager, Insumo.class);

        this.buscaTextual = new BuscaTextual<>(
            entityManager,
            Insumo.class,
            "SELECT * FROM oficina.insumos WHERE deletado = FALSE",
            "id",
            List.of("nome")
        );
    }

    public boolean obrigatorioParaServicoAtivo(final UUID id) {
        return this.entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "SELECT EXISTS (SELECT 1 FROM oficina.servicos_insumos_obrigatorios si "
                    + "JOIN oficina.servicos s ON s.id = si.servico_id "
                    + "WHERE si.insumo_id = :id AND s.deletado = FALSE)",
                Boolean.class
            )
            .setParameter("id", id)
            .getSingleResult();
    }

    public List<Insumo> listar(final @Nullable String nome, final @Nullable UUID after, final Pagina.Limite limite) {
        return this.buscaTextual.fulltextSearch(nome, after, limite);
    }
}
