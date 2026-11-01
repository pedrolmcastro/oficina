package com.oficina.api.infraestrutura.persistencia.insumo;

import com.oficina.api.dominio.insumo.Insumo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioInsumos {
    private final EntityManager entityManager;

    public RepositorioInsumos(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void adicionar(final Insumo insumo) {
        this.entityManager.persist(insumo);
    }

    public Optional<Insumo> buscar(final UUID id, final LockModeType modoBloqueio) {
        return Optional.ofNullable(this.entityManager.find(Insumo.class, id, modoBloqueio));
    }

    public List<Insumo> listar(final @Nullable String nome, final @Nullable UUID after, final int first) {
        final var sql = new StringBuilder("SELECT * FROM oficina.insumos WHERE deletado = FALSE");
        if (nome != null) {
            sql.append(" AND to_tsvector('portuguese', nome) @@ plainto_tsquery('portuguese', :nome)");
        }
        if (after != null) {
            sql.append(" AND id > :after");
        }
        sql.append(" ORDER BY id ASC");
        final var consulta = this.entityManager.unwrap(Session.class).createNativeQuery(sql.toString(), Insumo.class);
        if (nome != null) {
            consulta.setParameter("nome", nome);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(first).getResultList();
    }
}
