package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.aplicacao.Pagina;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;

public final class BuscaTextual<T> {
    private final EntityManager entityManager;
    private final Class<T> tipo;
    private final String sqlBase;
    private final String colunaId;
    private final String expressaoTexto;

    public BuscaTextual(
        final EntityManager entityManager,
        final Class<T> tipo,
        final String sqlBase,
        final String colunaId,
        final List<String> colunas
    ) {
        this.entityManager = entityManager;
        this.tipo = tipo;
        this.sqlBase = sqlBase;
        this.colunaId = colunaId;
        this.expressaoTexto = combinarColunas(colunas);
    }

    public List<T> fulltextSearch(
        final @Nullable String texto,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
        final var sql = new StringBuilder(this.sqlBase);
        if (texto != null) {
            sql
                .append(" AND to_tsvector('portuguese', ")
                .append(this.expressaoTexto)
                .append(") @@ plainto_tsquery('portuguese', :texto)");
        }
        if (after != null) {
            sql.append(" AND ").append(this.colunaId).append(" > :after");
        }
        sql.append(" ORDER BY ").append(this.colunaId).append(" ASC");
        final var consulta = this.entityManager.unwrap(Session.class).createNativeQuery(sql.toString(), this.tipo);
        if (texto != null) {
            consulta.setParameter("texto", texto);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(limite.quantidadeParaConsulta()).getResultList();
    }

    private static String combinarColunas(final List<String> colunas) {
        if (colunas.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma coluna para a busca textual");
        }

        return colunas.size() == 1
            ? colunas.getFirst()
            : colunas.stream().map(coluna -> "coalesce(" + coluna + ", '')").collect(Collectors.joining(" || ' ' || "));
    }
}
