package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioVeiculos extends RepositorioEntidades<Veiculo> {
    private final CriptografiaPlaca criptografia;

    public RepositorioVeiculos(final EntityManager entityManager, final CriptografiaPlaca criptografia) {
        super(entityManager, Veiculo.class);
        this.criptografia = criptografia;
    }

    public List<Veiculo> listar(
        final @Nullable Placa placa,
        final @Nullable UUID cliente,
        final @Nullable UUID after,
        final Pagina.Limite limite
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
            consulta.setParameter("hash", this.criptografia.calcularHash(placa));
        }
        if (cliente != null) {
            consulta.setParameter("cliente", cliente);
        }
        if (after != null) {
            consulta.setParameter("after", after);
        }
        return consulta.setMaxResults(limite.quantidadeParaConsulta()).getResultList();
    }
}
