package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.ordemservico.StatusOrdemServico;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioClientes extends RepositorioEntidades<Cliente> {
    private final CriptografiaDocumento criptografia;

    public RepositorioClientes(final EntityManager entityManager, final CriptografiaDocumento criptografia) {
        super(entityManager, Cliente.class);
        this.criptografia = criptografia;
    }

    public boolean possuiOrdemServicoAtiva(final UUID clienteId) {
        return this.entityManager
            .createQuery(
                "SELECT COUNT(o) FROM OrdemServico o WHERE o.clienteId = :cliente AND o.status <> :entregue",
                Long.class
            )
            .setParameter("cliente", clienteId)
            .setParameter("entregue", StatusOrdemServico.ENTREGUE)
            .getSingleResult() > 0;
    }

    public Optional<Cliente> buscar(final Documento documento) {
        final var cliente = this.entityManager
            .createQuery("FROM Cliente c WHERE c.documento.hash = :hash", Cliente.class)
            .setParameter("hash", this.criptografia.calcularHash(documento))
            .getSingleResultOrNull();

        return Optional.ofNullable(cliente);
    }
}
